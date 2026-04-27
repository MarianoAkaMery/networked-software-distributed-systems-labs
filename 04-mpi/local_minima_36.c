#include <stdio.h>
#include <stdlib.h>
#include <mpi.h>

// Group number: 36
// Group members:
// Salvatore Mariano Librici
// Rong Huang
// Mohammadali Amiri

const int N = 6;

// Allocates and initializes matrix
int *generate_matrix()
{
  int *A = (int *)malloc(N * N * sizeof(int));
  for (int i = 0; i < N * N; i++)
  {
    A[i] = rand() % 100;
  }
  return A;
}

// Returns the value at the given row and column
int val(int *A, int r, int c)
{
  return A[r * N + c];
}

int main(int argc, char **argv)
{
  MPI_Init(&argc, &argv);

  int rank, size;
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &size);

  if (N % size != 0)
  {
    if (rank == 0)
      printf("N must be a multiple of the number of processes.\n");
    MPI_Finalize();
    return 0;
  }

  // ------------------------------------------------------------
  // Generate matrix on rank 0
  // ------------------------------------------------------------

  int *fullA = NULL;
  if (rank == 0)
  {
    fullA = generate_matrix();
    // Save the matrix to file
    // DEBUG FOR US TO CHECK THE MATRIX
    FILE *output = fopen("matrix.txt", "w");
    if (output != NULL)
    {
      fprintf(output, "Generated Matrix (%d x %d):\n", N, N);
      for (int i = 0; i < N; i++)
      {
        for (int j = 0; j < N; j++)
        {
          fprintf(output, "%3d ", val(fullA, i, j));
        }
        fprintf(output, "\n");
      }
      fclose(output);
    }
  }

  // ------------------------------------------------------------
  // Distribute to all processes
  // ------------------------------------------------------------

  int rows_per_proc = N / size;
  int local_size = rows_per_proc * N;
  int *local = (int *)malloc(local_size * sizeof(int));
  MPI_Scatter(fullA, local_size, MPI_INT, local, local_size, MPI_INT, 0, MPI_COMM_WORLD);

  // ------------------------------------------------------------
  // Free global matrix
  // ------------------------------------------------------------

  if (rank == 0)
  {
    free(fullA);
    fullA = NULL;
  }

  // ------------------------------------------------------------
  // Exchange futher information if needed
  // ------------------------------------------------------------


  int *top_row = NULL;
  int *bottom_row = NULL;

  if (size > 1)
  {
    top_row = (int *)malloc(N * sizeof(int));
    bottom_row = (int *)malloc(N * sizeof(int));

    if (rank > 0)
    {
      MPI_Sendrecv(local, N, MPI_INT, rank - 1, 0,
                   top_row, N, MPI_INT, rank - 1, 1,
                   MPI_COMM_WORLD, MPI_STATUS_IGNORE);
    }

    if (rank < size - 1)
    {
      MPI_Sendrecv(local + (rows_per_proc - 1) * N, N, MPI_INT, rank + 1, 1,
                   bottom_row, N, MPI_INT, rank + 1, 0,
                   MPI_COMM_WORLD, MPI_STATUS_IGNORE);
    }
  }

  // ------------------------------------------------------------
  // Compute local minima (excluding GLOBAL borders)
  // ------------------------------------------------------------

  int *local_minima_counts = (int *)malloc(N * sizeof(int));

  for (int i = 0; i < rows_per_proc; i++)
  {
    int global_row = rank * rows_per_proc + i;
    int minima_in_row = 0;
    for (int j = 1; j < N - 1; j++)
    {
      int current = val(local, i, j);

      // Get values from adjacent cells
      int left = val(local, i, j - 1);
      int right = val(local, i, j + 1);

      int top;
      if (global_row > 0)
      {
        if (i == 0)
        {
          top = top_row[j];
        }
        else
        {
          top = val(local, i - 1, j);
        }
      }

      int bottom;
      if (global_row < N - 1)
      {
        if (i == rows_per_proc - 1)
        {
          bottom = bottom_row[j];
        }
        else
        {
          bottom = val(local, i + 1, j);
        }
      }

      if (global_row > 0 && global_row < N - 1)
      {
        if (current < left && current < right && current < top && current < bottom)
        {
          minima_in_row++;
        }
      }
    }

    local_minima_counts[global_row] = minima_in_row;
  }

  // ------------------------------------------------------------
  // Send results to rank 0 and print results on rank 0
  // ------------------------------------------------------------

  int *all_minima_counts = NULL;
  if (rank == 0)
  {
    all_minima_counts = (int *)malloc(N * sizeof(int));
  }

  MPI_Gather(local_minima_counts + rank * rows_per_proc, rows_per_proc, MPI_INT,
             all_minima_counts, rows_per_proc, MPI_INT, 0, MPI_COMM_WORLD);


  if (rank == 0)
  {
    for (int i = 0; i < N; i++)
    {
      printf("Row: %d, local minima: %d\n", i, all_minima_counts[i]);
    }
  }

  // ------------------------------------------------------------
  // Free allocated memory
  // ------------------------------------------------------------

  if (local != NULL)
  {
    free(local);
  }
  if (top_row != NULL)
  {
    free(top_row);
  }
  if (bottom_row != NULL)
  {
    free(bottom_row);
  }
  if (local_minima_counts != NULL)
  {
    free(local_minima_counts);
  }
  if (rank == 0 && all_minima_counts != NULL)
  {
    free(all_minima_counts);
  }

  MPI_Finalize();
  return 0;
}
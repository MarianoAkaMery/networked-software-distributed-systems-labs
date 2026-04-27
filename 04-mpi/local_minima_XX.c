#include <stdio.h>
#include <stdlib.h>
#include <mpi.h>

// Group number:
// Group members:

const int N = 256;

// Allocates and initializes matrix
int* generate_matrix() {
  int* A = (int*) malloc(N * N * sizeof(int));
  for (int i = 0; i < N * N; i++) {
    A[i] = rand() % 100;
  }
  return A;
}

// Returns the value at the given row and column
int val(int *A, int r, int c) {
  return A[r * N + c];
}

int main(int argc, char** argv) {
  MPI_Init(&argc, &argv);

  int rank, size;
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &size);

  if (N % size != 0) {
    if (rank == 0) printf("N must be a multiple of the number of processes.\n");
    MPI_Finalize();
    return 0;
  }

  // ------------------------------------------------------------
  // Generate matrix on rank 0
  // ------------------------------------------------------------

  int* fullA = NULL;
  // TODO

  // ------------------------------------------------------------
  // Distribute to all processes
  // ------------------------------------------------------------

  int* local = NULL;
  // TODO

  // ------------------------------------------------------------
  // Free global matrix
  // ------------------------------------------------------------

  // TODO
  
  // ------------------------------------------------------------
  // Exchange futher information if needed
  // ------------------------------------------------------------

  // TODO
  
  // ------------------------------------------------------------
  // Compute local minima (excluding GLOBAL borders)
  // ------------------------------------------------------------

  // TODO
  
  // ------------------------------------------------------------
  // Send results to rank 0 and print results on rank 0
  // ------------------------------------------------------------

  // TODO
  
  // ------------------------------------------------------------
  // Free allocated memory
  // ------------------------------------------------------------

  // TODO

  MPI_Finalize();
  return 0;
}

package it.polimi.nsds.eval;


/*
 * Group 36
 * Members:
 * Salvatore Mariano Librici
 * Rong Huang
 * Mohammadali Amiri
 */

import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.*;
import static org.apache.spark.sql.functions.*;

public class ProductAnalytics36 {

    public static void main(String[] args) throws Exception {

        
        SparkSession spark = SparkSession.builder()
                .master("local[8]")
                .appName("ProductAnalytics")
                .getOrCreate();

        spark.sparkContext().setLogLevel("ERROR");

        // ==========================================
        // Windows-specific configuration for Spark by Mariano
        
        System.setProperty("hadoop.home.dir", System.getProperty("user.dir"));
        System.setProperty("HADOOP_HOME", System.getProperty("user.dir"));
        spark.sparkContext().hadoopConfiguration().set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");

        // ==========================================

        // Load static datasets
        Dataset<Row> products = spark.read()
                .option("header", "true")
                .option("inferSchema", "true")
                .csv("input/products.csv")
                .cache();
        // ProductID, Category, Price

        Dataset<Row> historicalPurchases = spark.read()
                .option("header", "true")
                .option("inferSchema", "true")
                .csv("input/historicalPurchases.csv");
        // ProductID, Quantity

        // Aggregate once to avoid recomputation later
        Dataset<Row> historicalAgg = historicalPurchases
                .groupBy("ProductID")
                .agg(sum("Quantity").alias("totalQuantity"))
                .cache();

        

        // Streaming source: purchasesStream
        Dataset<Row> purchasesStream = spark.readStream()
                .format("rate")
                .option("rowsPerSecond", 5)
                .load();

        // Map the "value" counter to product IDs and quantities
        purchasesStream = purchasesStream
                .select(
                        col("timestamp"),
                        when(col("value").mod(10).lt(2), lit("P0582"))
                                .when(col("value").mod(10).lt(3), lit("P0112"))
                                .when(col("value").mod(10).lt(5), lit("P0536"))
                                .when(col("value").mod(10).equalTo(6), lit("P0742"))
                                .when(col("value").mod(10).equalTo(8), lit("P0027"))
                                .otherwise(lit("P00" + Math.random() % 100))
                                .alias("Product"),
                        (col("value").mod(5).plus(1)).alias("Quantity")       // 1–5 units
                );

        // ==========================================
        // Q1: Top 5 most purchased products (static)
        // Output: ProductID, totalQuantity
        // ==========================================

        Dataset<Row> topProducts = historicalAgg
                .orderBy(col("totalQuantity").desc())
                .limit(5)
                .cache();

        System.out.println("=== Q1: Top 5 most purchased products (historical) ===");
        topProducts.show(false);

        // ==========================================
        // Q2: Revenue per (Category, TopProduct)
        // ==========================================

        Dataset<Row> revenue = topProducts
                .join(products, "ProductID")
                .select(
                        col("Category"),
                        col("ProductID"),
                        col("totalQuantity"),
                        col("Price").multiply(col("totalQuantity")).alias("totalRevenue")
                );

        System.out.println("=== Q2: Total revenue for top products ===");
        revenue.select("Category", "ProductID", "totalRevenue").show(false);

        // ==========================================
        // Q3: Streaming window for top products
        // ==========================================

        Dataset<Row> streamTopProducts = purchasesStream
                .join(
                        topProducts.select("ProductID"),
                        purchasesStream.col("Product").equalTo(topProducts.col("ProductID")),
                        "inner"
                )
                .drop(topProducts.col("ProductID"));

        Dataset<Row> windowed = streamTopProducts
                .withWatermark("timestamp", "1 minute")
                .groupBy(
                        window(col("timestamp"), "15 seconds", "5 seconds"),
                        col("Product")
                )
                .agg(sum("Quantity").alias("streamQuantity"));

        windowed
                .select("window", "Product", "streamQuantity")
                .writeStream()
                .outputMode("update")             
                .format("console")
                .option("truncate", "false")
                .option("numRows", 50)
                .queryName("Q3_TopProductsWindowed")
                .start();

        // ==========================================
        // Q4: Difference historical vs streaming
        // ==========================================

        Dataset<Row> diff = windowed
                .join(
                        topProducts,
                        windowed.col("Product").equalTo(topProducts.col("ProductID")),
                        "inner"
                )
                .select(
                        windowed.col("window"),
                        windowed.col("Product"),
                        topProducts.col("totalQuantity"),
                        windowed.col("streamQuantity"),
                        topProducts.col("totalQuantity")
                                .minus(windowed.col("streamQuantity"))
                                .alias("difference")
                );

        StreamingQuery q4Query = diff
                .writeStream()
                .outputMode("update")             
                .format("console")
                .option("truncate", "false")
                .option("numRows", 50)
                .queryName("Q4_DifferenceStaticVsStream")
                .start();

        // Keep the application running
        q4Query.awaitTermination();
    }
}

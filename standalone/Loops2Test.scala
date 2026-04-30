import java.util.concurrent._

import scala.scalanative.meta.LinktimeInfo._

object Test {

  def main(args: Array[String]): Unit = {
    println("Hello, World!")
    println(
      s"""
       Linktime Info collection:
         debugMode: ${debugMode}
         releaseMode: ${releaseMode}
         runtimeVersion: ${runtimeVersion}
         garbageCollector: ${garbageCollector}
         isWeakReferenceSupported: ${isWeakReferenceSupported}
         isMultithreadingEnabled: ${isMultithreadingEnabled}
         isContinuationsSupported: ${isContinuationsSupported}
       ForkJoinPool.commonPool() info:
         POOL_PARALLELISM: ${ForkJoinPool.commonPool().getParallelism()}
         POOL_ASYNC_MODE: ${ForkJoinPool.commonPool().getAsyncMode()}
         POOL_SIZE: ${ForkJoinPool.commonPool().getPoolSize()}
       """.stripIndent()
    )
    println("")

    val WARMUP_RUNS = 5
    val BENCHMARK_RUNS = 100
    val NPS: Long = 1000L * 1000 * 1000
    val ITEMS = 1 << 20

    println(s"-- Benchmark for SubmissionPublisherLoops2Test --")
    println("-- Warming up ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops2Test(ITEMS).main(),
      WARMUP_RUNS,
      timeoutSecs = 10L,
      verbose = true
    )
    println("-- Running benchmark ...")
    val (_, _, _, times2) = loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops2Test(ITEMS).main(),
      BENCHMARK_RUNS,
      timeoutSecs = 10L,
      verbose = false
    )
    println("-- Statistics:")
    println(f" Average time: ${average(times2)}%7.3f seconds")
    println(f" Std Dev time: ${stddev(times2)}%7.3f seconds")
    println(s"-- End of SubmissionPublisherLoops2Test session --")
    println("")

  }

  def average(xs: List[Double]): Double =
    xs.sum / xs.size

  def stddev(xs: List[Double]): Double = {
    val mean = average(xs)
    math.sqrt(
      xs.map(x => math.pow(x - mean, 2)).sum / (xs.size - 1)
    )
  }

  @inline
  def loopStatisticsWithTimeout(
      func: () => Unit,
      count: Int,
      timeoutSecs: Long,
      verbose: Boolean
  ): (Int, Int, Int, List[Double]) = {
    var successCount = 0
    var timeoutCount = 0
    var failureCount = 0
    var times = List[Double]()

    for (i <- 0 until count) {
      try {
        val tic = System.nanoTime()
        val task = CompletableFuture
          .runAsync(() => { func() })
          .orTimeout(timeoutSecs, TimeUnit.SECONDS)
          .join()
        val toc = System.nanoTime()
        val timeSeconds = (toc - tic).toDouble / (1000L * 1000 * 1000)
        times = times.appended(timeSeconds)
        if (verbose || (i % 10 == 0))
          println(
            f"Iteration ${i + 1}: Success, per step time: ${timeSeconds}%7.3f seconds"
          )
        successCount += 1
      } catch {
        case ex: CompletionException => {
          val cause = ex.getCause()
          if (cause.isInstanceOf[TimeoutException]) {
            timeoutCount += 1
            if (verbose) println(f"Iteration ${i + 1}: Timeout")
          } else {
            failureCount += 1
            System.err.println(
              f"Iteration ${i + 1}: Failure, exception: ${cause}"
            )
          }
        }
      } finally {
        Thread.sleep(500L)
      }
    }

    println(
      f"Loop completed: ${successCount} successes (${successCount / count}%3.4f), ${timeoutCount} timeouts (${timeoutCount / count}%3.4f), ${failureCount} failures, total ${count} iterations."
    )
    (successCount, timeoutCount, failureCount, times)
  }

}

/** One FJ publisher, many subscribers */
class SubmissionPublisherLoops2Test(items: Int) {

  val ITEMS: Int = items
  /* Original JSR-166 parameters: */
  val CONSUMERS = 64

  val CAP: Int = Flow.defaultBufferSize()
  val phaser = new Phaser(CONSUMERS + 1)

  final class Sub extends Flow.Subscriber[Boolean] {
    var sn: Flow.Subscription = null
    var count = 0

    def onSubscribe(s: Flow.Subscription): Unit = {
      sn = s
      s.request(CAP)
    }

    def onNext(t: Boolean): Unit = {
      if (({ count += 1; count } & (CAP - 1)) == (CAP >>> 1))
        sn.request(CAP)
    }

    def onError(t: Throwable): Unit = {
      t.printStackTrace()
    }

    def onComplete(): Unit = {
      if (count != ITEMS)
        System.err.println("Error: remaining " + (ITEMS - count))
      phaser.arrive()
    }
  }

  final class Pub extends RecursiveAction {
    final val pub =
      new SubmissionPublisher[Boolean](ForkJoinPool.commonPool(), CAP)

    def compute(): Unit = {
      val p = pub
      for (i <- 0 until CONSUMERS) {
        p.subscribe(new Sub())
      }
      for (i <- 0 until ITEMS) {
        p.submit(true)
      }
      p.close()
    }
  }

  def main(): Unit = {
    // System.out.println(
    //   "ITEMS: " + ITEMS + " CONSUMERS: " + CONSUMERS + " CAP: " + CAP
    // )
    oneRun()
  }

  def oneRun(): Unit = {
    new Pub().fork()
    phaser.arriveAndAwaitAdvance()
  }
}

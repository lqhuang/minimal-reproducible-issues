//> using scala 3.8.3
//> using platform native

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

    println(s"-- Benchmark for SubmissionPublisherLoops1Test --")
    println("-- Warming up ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops1Test(ITEMS).main(),
      WARMUP_RUNS,
      timeoutSecs = 10L,
      verbose = true
    )
    println("-- Running benchmark ...")
    val (_, _, _, times1) = loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops1Test(ITEMS).main(),
      BENCHMARK_RUNS,
      timeoutSecs = 10L,
      verbose = false
    )
    println("-- Statistics:")
    println(f" Average time: ${average(times1)}%7.3f seconds")
    println(f" Std Dev time: ${stddev(times1)}%7.3f seconds")
    println(s"-- End of SubmissionPublisherLoops1Test session --")
    println("")

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

    println(s"-- Benchmark for SubmissionPublisherLoops3Test --")
    println("-- Warming up ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops3Test(ITEMS).main(),
      WARMUP_RUNS,
      timeoutSecs = 10L,
      verbose = true
    )
    println("-- Running benchmark ...")
    val (_, _, _, times3) = loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops3Test(ITEMS).main(),
      BENCHMARK_RUNS,
      timeoutSecs = 10L,
      verbose = false
    )
    println("-- Statistics:")
    println(f" Average time: ${average(times3)}%7.3f seconds")
    println(f" Std Dev time: ${stddev(times3)}%7.3f seconds")
    println(s"-- End of SubmissionPublisherLoops3Test session --")
    println("")

    // SubmissionPublisherLoops4Test
    println(s"-- Benchmark for SubmissionPublisherLoops4Test --")
    println("-- Warming up ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops4Test(ITEMS).main(),
      WARMUP_RUNS,
      timeoutSecs = 300L,
      verbose = true
    )
    println("-- Running benchmark ...")
    val (_, _, _, times4) = loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops4Test(ITEMS).main(),
      20,
      timeoutSecs = 300L,
      verbose = false
    )
    println("-- Statistics:")
    println(f" Average time: ${average(times4)}%7.3f seconds")
    println(f" Std Dev time: ${stddev(times4)}%7.3f seconds")
    println(s"-- End of SubmissionPublisherLoops4Test session --")

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
            if (verbose || (i % 10 == 0))
              println(f"Iteration ${i + 1}: Timeout")
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
      f"Loop completed: ${successCount} successes (${successCount.toDouble / count}%3.4f), ${timeoutCount} timeouts (${timeoutCount.toDouble / count}%3.4f), ${failureCount} failures (${failureCount.toDouble / count}%3.4f), total ${count} iterations."
    )
    (successCount, timeoutCount, failureCount, times)
  }

}

/** One publisher, many subscribers */
class SubmissionPublisherLoops1Test(items: Int) {

  val ITEMS: Int = items
  /* Parameters for multiple threading (original JSR-166 setup) */
  val CONSUMERS = 64

  val CAP: Int = Flow.defaultBufferSize()
  val phaser = new Phaser(CONSUMERS + 1)

  final class Sub extends Flow.Subscriber[Boolean] {
    var sn: Flow.Subscription = null
    var count = 0

    override def onSubscribe(s: Flow.Subscription): Unit = {
      sn = s
      s.request(CAP)
    }

    override def onNext(t: Boolean): Unit = {
      if (({ count += 1; count } & (CAP - 1)) == (CAP >>> 1))
        sn.request(CAP)
    }

    override def onError(t: Throwable): Unit = {
      t.printStackTrace()
    }

    override def onComplete(): Unit = {
      if (count != ITEMS)
        System.err.println("Error: remaining " + (ITEMS - count))
      phaser.arrive()
    }
  }

  def main(): Unit = {
    // System.out.println(
    //   "ITEMS: " + ITEMS + " CONSUMERS: " + CONSUMERS + " CAP: " + CAP
    // )
    val exec = ForkJoinPool.commonPool()
    oneRun(exec)
    if (exec ne ForkJoinPool.commonPool()) exec.shutdown()
  }

  def oneRun(exec: ExecutorService): Unit = {
    val pub = new SubmissionPublisher[Boolean](exec, CAP)
    for (i <- 0 until CONSUMERS) {
      pub.subscribe(new Sub())
    }
    for (i <- 0 until ITEMS) {
      pub.submit(true)
    }
    pub.close()
    phaser.arriveAndAwaitAdvance()
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

/** Creates PRODUCERS publishers each with CONSUMERS subscribers, each sent
  * ITEMS items, with CAP buffering; repeats REPS times
  */
class SubmissionPublisherLoops3Test(items: Int) {

  val ITEMS: Int = items
  /* Original JSR-166 parameters: */
  val PRODUCERS = 32
  val CONSUMERS = 32

  val CAP: Int = Flow.defaultBufferSize()
  val phaser = new Phaser(PRODUCERS * CONSUMERS + 1)

  def main(): Unit = {
    // System.out.println(
    //   "ITEMS: " + ITEMS + " PRODUCERS: " + PRODUCERS + " CONSUMERS: " + CONSUMERS +
    //     " CAP: " + CAP
    // )
    oneRun()
  }

  def oneRun(): Unit = {
    val nitems = ITEMS.toLong * PRODUCERS * CONSUMERS
    val startTime = System.nanoTime()
    for (i <- 0 until PRODUCERS) {
      new Pub().fork()
    }
    phaser.arriveAndAwaitAdvance()
    val elapsed = System.nanoTime() - startTime
    val secs = elapsed.toDouble / (1000L * 1000 * 1000)
    val ips = nitems / secs
    // System.out.println(f"  items per sec: ${ips}%14.2f")
  }

  final class Sub extends Flow.Subscriber[Boolean] {
    var count = 0
    var subscription: Flow.Subscription = null

    def onSubscribe(s: Flow.Subscription): Unit = {
      subscription = s
      s.request(CAP)
    }

    def onNext(b: Boolean): Unit = {
      if (b && ({ count += 1; count } & ((CAP >>> 1) - 1)) == 0)
        subscription.request(CAP >>> 1)
    }

    def onComplete(): Unit = {
      if (count != ITEMS)
        System.err.println("Error: remaining " + (ITEMS - count))
      phaser.arrive()
    }

    def onError(t: Throwable): Unit = {
      t.printStackTrace()
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
}

/** Creates PRODUCERS publishers each with PROCESSORS processors each with
  * CONSUMERS subscribers, each sent ITEMS items, with max CAP buffering;
  * repeats REPS times
  */
class SubmissionPublisherLoops4Test(items: Int) {

  val ITEMS: Int = items
  /* Original JSR-166 parameters: */
  val PRODUCERS = 32
  val PROCESSORS = 32
  val CONSUMERS = 32

  val CAP: Int = Flow.defaultBufferSize()
  val SINKS: Int = PRODUCERS * PROCESSORS * CONSUMERS
  val phaser = new Phaser(SINKS + 1)

  def main(): Unit = {
    // System.out.println(
    //   "ITEMS: " + ITEMS +
    //     " PRODUCERS: " + PRODUCERS +
    //     " PROCESSORS: " + PROCESSORS +
    //     " CONSUMERS: " + CONSUMERS +
    //     " CAP: " + CAP
    // )
    oneRun()
  }

  def oneRun(): Unit = {
    val total: Long = ITEMS.toLong * SINKS
    val startTime = System.nanoTime()
    for (i <- 0 until PRODUCERS) {
      new Pub().fork()
    }
    phaser.arriveAndAwaitAdvance()
    val elapsed = System.nanoTime() - startTime
    val secs = elapsed.toDouble / (1000L * 1000 * 1000)
    val ips = total / secs
    // System.out.println(f"  items per sec: ${ips}%14.2f")
  }

  final class Sub extends Flow.Subscriber[Boolean] {
    var count = 0
    var subscription: Flow.Subscription = null

    def onSubscribe(s: Flow.Subscription): Unit = {
      subscription = s
      s.request(CAP)
    }

    def onNext(b: Boolean): Unit = {
      if (b && ({ count += 1; count } & ((CAP >>> 1) - 1)) == 0)
        subscription.request(CAP >>> 1)
    }

    def onComplete(): Unit = {
      if (count != ITEMS)
        System.err.println("Error: remaining " + (ITEMS - count))
      phaser.arrive()
    }

    def onError(t: Throwable): Unit = {
      t.printStackTrace()
    }
  }

  final class Proc(executor: Executor, maxBufferCapacity: Int)
      extends SubmissionPublisher[Boolean](executor, maxBufferCapacity)
      with Flow.Processor[Boolean, Boolean] {
    var subscription: Flow.Subscription = null
    var count = 0

    def onSubscribe(subscription: Flow.Subscription): Unit = {
      this.subscription = subscription
      subscription.request(CAP)
    }

    def onNext(item: Boolean): Unit = {
      if (({ count += 1; count } & ((CAP >>> 1) - 1)) == 0)
        subscription.request(CAP >>> 1)

      submit(item)
    }

    def onError(ex: Throwable): Unit = {
      closeExceptionally(ex)
    }

    def onComplete(): Unit = {
      close()
    }
  }

  final class Pub extends RecursiveAction {
    final val pub =
      new SubmissionPublisher[Boolean](ForkJoinPool.commonPool(), CAP)

    def compute(): Unit = {
      val p = pub
      for (j <- 0 until PROCESSORS) {
        val t = new Proc(ForkJoinPool.commonPool(), CAP)
        for (i <- 0 until CONSUMERS) {
          t.subscribe(new Sub())
        }
        p.subscribe(t)
      }
      for (i <- 0 until ITEMS) {
        p.submit(true)
      }
      p.close()
    }
  }
}

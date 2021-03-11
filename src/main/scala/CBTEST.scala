import cats.effect.{ExitCode, Async}
import cats.implicits._
import monix.eval.Task
import monix.eval.TaskApp
import monix.catnap._
//import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration._
import monix.execution.exceptions.ExecutionRejectedException
import monix.execution.Scheduler
import monix.execution.ExecutionModel

import java.util.concurrent.Executors

object CBTEST extends TaskApp {
  System.setProperty("monix.eval.stackTracingMode", "full")
  case class CustomError(msg: String) extends Exception(msg)

  //override
  lazy val scheduler2 = {
    val javaService = Executors.newScheduledThreadPool(10)
    Scheduler(javaService, ExecutionModel.AlwaysAsyncExecution)
  }

  override def run(args: List[String]): Task[ExitCode] = {

    val resetTimeout = 5.seconds
    val circuitBreaker = CircuitBreaker[Task]
      .of(
        maxFailures = 5,
        resetTimeout = resetTimeout,
        exponentialBackoffFactor = 1,
        maxResetTimeout = 30.seconds,
        onRejected = Task {
          println("Task rejected in Open or HalfOpen")
        },
        onClosed = Task {
          println("Switched to Close, accepting tasks again")
        },
        onHalfOpen = Task {
          println("Switched to HalfOpen, accepted one task for testing")
        },
        onOpen = Task {
          println(
            s"Switched to Open, all incoming tasks rejected for the next $resetTimeout"
          )
        }
      )

    val problematic: Task[Unit] = Task {
      val nr = util.Random.nextInt()
      if (nr % 2 == 0) {
        ()
      } else {
        throw new RuntimeException("dummy")
      }
    }

    val problematic2: Task[Unit] = Task.raiseError(CustomError("error"))

    def protectWithRetry2[F[_], A](task: F[A], cb: CircuitBreaker[F])(implicit
        F: Async[F]
    ): F[A] = {
      (Async.shift[F](scheduler2)) *> F.suspend(cb.protect(task).recoverWith {
        case e: CustomError => //ExecutionRejectedException
          // Waiting for the CircuitBreaker to close, then retry
          println(s"awaitClose retry $e")
            .pure[F] *> cb.awaitClose *> println(s"after awaitClose")
            .pure[F] *> protectWithRetry2(task, cb)
      })
    }

    def protectWithRetry[A](
        task: Task[A],
        cb: CircuitBreaker[Task]
    )(implicit scheduler: Scheduler): Task[A] = {
      Task.shift(scheduler2) *> Task.fromFuture(
        cb.protect(task)
          .onErrorRestartLoop(0) { (e, times, retry) =>
            // Retrying for a maximum of 5 times
            if (times < 10) {
              (cb.state >>= (s =>
                Task(println(s"On error retry $times times : ${s}"))
              )) *> (Task.fromFuture({
                val f = cb.awaitClose.runToFuture(scheduler)
                f.onComplete { _ =>
                  println("onComplete")
                }
                f
              })) *> Task(println(s"after awaitClose")) *> retry(times + 1)
            } else {
              Task(println(s"On stop retry $e")) *> Task.raiseError(e)
            }
          }
          .runToFuture
      )
    }

    {
      circuitBreaker >>= (cb => protectWithRetry(problematic2, cb)(scheduler))
    }.as(ExitCode.Success)
  }

}

interp.load.ivy("co.fs2" %% "fs2-core" % "1.0.2")
interp.load.ivy("co.fs2" %% "fs2-io" % "1.0.2")

import cats.effect.IO
import cats.effect.ContextShift
import fs2._

implicit val ioContextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
val currentTime: IO[Long] = IO.delay {System.currentTimeMillis}


def disablePrettyPrintIO = repl.pprinter.update(repl.pprinter().copy(additionalHandlers = {
  case io: cats.effect.IO[_] => pprint.Tree.Literal("✨✨✨")
}))

disablePrettyPrintIO

def enablePrettyPrintIO = repl.pprinter.update(repl.pprinter().copy(additionalHandlers = PartialFunction.empty))
// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.tortoise

import org.scalatest.FunSuite

class CompilerTests extends FunSuite {

  test("clear-all") {
    import Compiler.{compileCommands => compile}
    assertResult("world.clearall()") {
      compile("clear-all") }
  }

  test("literals") {
    import Compiler.{compileReporter => compile}
    assertResult("1")(
      compile("1"))
    assertResult("1")(
      compile("1.0"))
    assertResult("[]")(
      compile("[]"))
    assertResult("[1, [2], 3]")(
      compile("[1 [2] 3]"))
  }

  test("arithmetic expressions") {
    import Compiler.{compileReporter => compile}
    assertResult("(2 + 2)")(
      compile("2 + 2"))
    assertResult("((1 + 2) * 3)")(
      compile("(1 + 2) * 3"))
    assertResult("(1 + (2 * 3))")(
      compile("1 + 2 * 3"))
    assertResult("((1 + 2) + (3 + 4))")(
      compile("(1 + 2) + (3 + 4)"))
  }

  test("equality"){
    import Compiler.{compileReporter => compile}
    assertResult("(2 === 2)")(compile("2 = 2"))
    assertResult("""("hello" === "hello")""")(compile(""""hello" = "hello""""))
  }

  test("list construction") {
    import Compiler.{compileReporter => compile}
    assertResult("Prims.list(1)")(compile("(list 1)"))
    assertResult("Prims.list(1, 2)")(compile("list 1 2"))
    assertResult("Prims.list(world.minPxcor)")(compile("(list min-pxcor)"))
  }

  test("max") {
    import Compiler.{compileReporter => compile}
    assertResult("Prims.max([1, 2, 3])")(compile("max [1 2 3]"))
  }

  test("commands: arithmetic + printing") {
    import Compiler.{compileCommands => compile}
    val expected = """|Prims.outputprint((2 + 2))
                      |Prims.outputprint((3 * 3))""".stripMargin
    assertResult(expected)(
      compile("output-print 2 + 2 output-print 3 * 3"))
  }

  test("commands: turtle creation") {
    import Compiler.{compileCommands => compile}
    val expected = """|AgentSet.ask(world.createorderedturtles(5, ""), true, function(){  });
                      |Prims.outputprint(AgentSet.count(world.turtles()))""".stripMargin
    assertResult(expected)(
      compile("cro 5 output-print count turtles"))
  }

  test("commands: while true") {
    import Compiler.{compileCommands => compile}
    val input = "while [true] [output-print 0]"
    val expected =
      """while (true) {
        |Prims.outputprint(0)
        |}""".stripMargin
    assertResult(expected)(compile(input))
  }

  test("commands: let") {
    import Compiler.{compileCommands => compile}
    val input = "let x 5 output-print x"
    val expected = """|var X = 5;
                      |Prims.outputprint(X)""".stripMargin
    assertResult(expected)(compile(input))
  }

  test("command procedure") {
    import Compiler.{compileProcedures => compile}
    val input = "to foo output-print 5 end"
    val expected = """world = new World(0, 0, 0, 0, 12.0, 0);
                     |function FOO () {
                     |Prims.outputprint(5)
                     |};
                     |""".stripMargin
    assertResult(expected)(compile(input)._1)
  }

  test("commands: ask simple") {
    import Compiler.{compileCommands => compile}
    val input = "ask turtles [fd 1]"
    val expected = "AgentSet.ask(world.turtles(), true, function(){ Prims.fd(1) });"
    assertResult(expected)(compile(input))
  }

  test("commands: ask with turtle variable") {
    import Compiler.{compileCommands => compile}
    val input = "ask turtles [output-print xcor]"
    val expected = "AgentSet.ask(world.turtles(), true, function(){ Prims.outputprint(AgentSet.getTurtleVariable(3)) });"
    assertResult(expected)(compile(input))
  }

  test("commands: die") {
    import Compiler.{compileCommands => compile}
    val input = "ask turtles [die]"
    val expected = "AgentSet.ask(world.turtles(), true, function(){ AgentSet.die() });"
    assertResult(expected)(compile(input))
  }

  test("commands: ask patches with variable") {
    import Compiler.{compileCommands => compile}
    val input = "ask patches [output-print pxcor]"
    val expected = "AgentSet.ask(world.patches(), true, function(){ Prims.outputprint(AgentSet.getPatchVariable(0)) });"
    assertResult(expected)(compile(input))
  }

  test("globals: access") {
    import Compiler.{compileProcedures => compile}
    val input = "globals [x y z] to foo-bar? output-print z output-print y output-print x end"
    val expected =
     """|Globals.init(3)
        |world = new World(0, 0, 0, 0, 12.0, 0);
        |function FOO_BAR_P () {
        |Prims.outputprint(Globals.getGlobal(2))
        |Prims.outputprint(Globals.getGlobal(1))
        |Prims.outputprint(Globals.getGlobal(0))
        |};
        |""".stripMargin
    assertResult(expected)(compile(input)._1)
  }

  test("globals: set") {
    import Compiler.{compileProcedures => compile}
    val input = "globals [x] to foo set x 5 output-print x end"
    val expected =
     """|Globals.init(1)
        |world = new World(0, 0, 0, 0, 12.0, 0);
        |function FOO () {
        |Globals.setGlobal(0,5)
        |Prims.outputprint(Globals.getGlobal(0))
        |};
        |""".stripMargin
    assertResult(expected)(compile(input)._1)
  }

  test("commands: ask turtles to set color") {
    import Compiler.{compileCommands => compile}
    val input = "ask turtles [set color green]"
    val expected = "AgentSet.ask(world.turtles(), true, function(){ AgentSet.setTurtleVariable(1,55) });"
    assertResult(expected)(compile(input))
  }

  test("commands: ask turtles to set pcolor") {
    import Compiler.{compileCommands => compile}
    val input = "ask turtles [set pcolor green]"
    val expected = "AgentSet.ask(world.turtles(), true, function(){ AgentSet.setPatchVariable(2,55) });"
    assertResult(expected)(compile(input))
  }

  test("commands: ask patches to set pcolor") {
    import Compiler.{compileCommands => compile}
    val input = "ask patches [set pcolor green]"
    val expected = "AgentSet.ask(world.patches(), true, function(){ AgentSet.setPatchVariable(2,55) });"
    assertResult(expected)(compile(input))
  }

  test("commands: with") {
    import Compiler.{compileCommands => compile}
    val input = "ask patches with [pxcor = 1] [output-print pycor]"
    val expectedAgentFilter =
      "AgentSet.agentFilter(world.patches(), function(){ return (AgentSet.getPatchVariable(0) === 1) })"
    val expected = s"AgentSet.ask($expectedAgentFilter, true, function(){ Prims.outputprint(AgentSet.getPatchVariable(1)) });"
    assertResult(expected)(compile(input))
  }

  test("reporters: word") {
    import Compiler.{compileReporter => compile}
    val input = "(word 1 2 3)"
    val expected = """(Dump("") + Dump(1) + Dump(2) + Dump(3))"""
    assertResult(expected)(compile(input))
  }

}

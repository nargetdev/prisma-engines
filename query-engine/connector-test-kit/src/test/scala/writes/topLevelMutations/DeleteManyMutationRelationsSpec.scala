package writes.topLevelMutations

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class DeleteManyMutationRelationsSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "a P0 to C1! relation " should "error when deleting the parent" in {

    val schema = """model Parent{
                            id String @id @default(cuid())
                            p  String @unique
                        }

                        model Child{
                            id        String @id @default(cuid())
                            c         String @unique
                            parentReq Parent @relation(references: [id])
                        }"""

    val project = ProjectDsl.fromString { schema }
    database.setup(project)

    server
      .query(
        """mutation {
          |  createChild(data: {
          |    c: "c1"
          |    parentReq: {
          |      create: {p: "p1"}
          |    }
          |  }){
          |    id
          |  }
          |}""".stripMargin,
        project
      )

    server.queryThatMustFail(
      s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |  count
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation 'ChildToParent' between Child and Parent"
    )

  }

  "a P0 to C1! relation " should "error when deleting the parent with empty filter" in {
    val schema = """model Parent{
                            id String @id @default(cuid())
                            p  String @unique
                        }

                        model Child{
                            id        String @id @default(cuid())
                            c         String @unique
                            parentReq Parent @relation(references: [id])
                        }"""

    val project = ProjectDsl.fromString { schema }
    database.setup(project)

    server
      .query(
        """mutation {
          |  createChild(data: {
          |    c: "c1"
          |    parentReq: {
          |      create: {p: "p1"}
          |    }
          |  }){
          |    id
          |  }
          |}""".stripMargin,
        project
      )

    server.queryThatMustFail(
      s"""
         |mutation {
         |  deleteManyParents(
         |  where: {}
         |  ){
         |  count
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation 'ChildToParent' between Child and Parent"
    )

  }

  "a P1! to C1! relation " should "error when deleting the parent" in {
    schemaWithRelation(onParent = ChildReq, onChild = ParentReq).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      val res = server
        .query(
          s"""mutation {
          |  createParent(data: {
          |    p: "p1"
          |    p_1: "p_1"
          |    p_2: "p_2"
          |    childReq: {
          |      create: {
          |        c: "c1"
          |        c_1: "c_1"
          |        c_2: "c_2"
          |      }
          |    }
          |  }){
          |    ${t.parent.selection}
          |    childReq{
          |       ${t.child.selection}
          |    }
          |  }
          |}""".stripMargin,
          project
        )
      val childId  = t.child.where(res, "data.createParent.childReq")
      val parentId = t.parent.where(res, "data.createParent")

      server.queryThatMustFail(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: $parentId
         |  ) {
         |    count
         |  }
         |}
      """.stripMargin,
        project,
        errorCode = 3042,
        errorContains = "The change you are trying to make would violate the required relation 'ChildToParent' between Child and Parent"
      )

    }
  }

  "a P1! to C1 relation" should "succeed when trying to delete the parent" in {
    schemaWithRelation(onParent = ChildReq, onChild = ParentOpt, withoutParams = true).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      val res = server
        .query(
          s"""mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childReq: {
          |      create: {
          |        c: "c1"
          |        c_1: "c_1",
          |        c_2: "c_2"
          |      }
          |    }
          |  }){
          |    childReq{
          |       c
          |    }
          |  }
          |}""".stripMargin,
          project
        )

      /*val parentId = t.parent.where(res, "data.createParent")*/

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |    where: {
         |      p: "p1"
         |    }
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )
    }
  }

  "a P1 to C1  relation " should "succeed when trying to delete the parent" in {
    schemaWithRelation(onParent = ChildOpt, onChild = ParentOpt, withoutParams = true).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      val res = server
        .query(
          s"""mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childOpt: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    p
          |    childOpt{
          |       c
          |    }
          |  }
          |}""".stripMargin,
          project
        )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |    where: {
         |      p: "p1"
         |    }
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )
    }
  }

  "a P1 to C1  relation " should "succeed when trying to delete the parent if there are no children" in {
    schemaWithRelation(onParent = ChildOpt, onChild = ParentOpt).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server
        .query(
          s"""mutation {
          |  createParent(data: {
          |    p: "p1"
          |  }){
          |    ${t.parent.selection}
          |  }
          |}""".stripMargin,
          project
        )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |    where: {p: "p1"}
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )

    }
  }

  "a PM to C1!  relation " should "error when deleting the parent" in {
    schemaWithRelation(onParent = ChildList, onChild = ParentReq).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    p_1: "p_1"
        |    p_2: "p_2"
        |    childrenOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      server.queryThatMustFail(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |  count
         |  }
         |}
      """.stripMargin,
        project,
        errorCode = 3042,
        errorContains = "The change you are trying to make would violate the required relation 'ChildToParent' between Child and Parent"
      )

    }
  }

  "a PM to C1!  relation " should "succeed if no child exists that requires the parent" in {
    schemaWithRelation(onParent = ChildList, onChild = ParentReq).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |  count
         |  }
         |}
      """.stripMargin,
        project
      )

    }

  }

  "a P1 to C1!  relation " should "error when trying to delete the parent" in {
    schemaWithRelation(onParent = ChildOpt, onChild = ParentReq).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    p_1: "p_1"
        |    p_2: "p_2"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      server.queryThatMustFail(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |  count
         |  }
         |}
      """.stripMargin,
        project,
        errorCode = 3042,
        errorContains = "The change you are trying to make would violate the required relation 'ChildToParent' between Child and Parent"
      )

    }
  }

  "a P1 to C1!  relation " should "succeed when trying to delete the parent if there is no child" in {
    schemaWithRelation(onParent = ChildOpt, onChild = ParentReq).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |
        |  }){
        |    p
        |  }
        |}""".stripMargin,
        project
      )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |  count
         |  }
         |}
      """.stripMargin,
        project
      )

    }
  }

  "a PM to C1 " should "succeed in deleting the parent" in {
    schemaWithRelation(onParent = ChildList, onChild = ParentOpt).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server
        .query(
          """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childrenOpt: {
          |      create: [{c: "c1"}, {c: "c2"}]
          |    }
          |  }){
          |    childrenOpt{
          |       c
          |    }
          |  }
          |}""".stripMargin,
          project
        )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: { p: "p1"}
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )

    }
  }

  "a PM to C1 " should "succeed in deleting the parent if there is no child" in {
    schemaWithRelation(onParent = ChildList, onChild = ParentOpt).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server
        .query(
          """mutation {
          |  createParent(data: {
          |    p: "p1"
          |  }){
          |    p
          |  }
          |}""".stripMargin,
          project
        )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: { p: "p1"}
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )

    }
  }

  "a P1! to CM  relation" should "should succeed in deleting the parent " in {
    schemaWithRelation(onParent = ChildReq, onChild = ParentList).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childReq: {
        |      create: {
        |        c: "c1"
        |        c_1: "c_1"
        |        c_2: "c_2"
        |      }
        |    }
        |  }){
        |    childReq{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )
    }
  }

  "a P1 to CM  relation " should " should succeed in deleting the parent" in {
    schemaWithRelation(onParent = ChildOpt, onChild = ParentList).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )
    }
  }

  "a P1 to CM  relation " should " should succeed in deleting the parent if there is no child" in {
    schemaWithRelation(onParent = ChildOpt, onChild = ParentList).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |
        |  }){
        |    p
        |  }
        |}""".stripMargin,
        project
      )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )
    }
  }

  "a PM to CM  relation" should "succeed in deleting the parent" in {
    schemaWithRelation(onParent = ChildList, onChild = ParentList).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )
    }
  }

  "a PM to CM  relation" should "succeed in deleting the parent if there is no child" in {
    schemaWithRelation(onParent = ChildList, onChild = ParentList).test { t =>
      val project = SchemaDsl.fromStringV11() {
        t.datamodel
      }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |
        |  }){
        |    p
        |  }
        |}""".stripMargin,
        project
      )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: {p: "p1"}
         |  ){
         |    count
         |  }
         |}
      """.stripMargin,
        project
      )
    }
  }

  "a PM to CM  relation" should "delete the parent from other relations as well" in {
    val testDataModels = {
      val dm1 = """model Parent{
                       id           String @id @default(cuid())
                       p            String @unique
                       childrenOpt  Child[] @relation(references: [id])
                       stepChildOpt StepChild @relation(references: [id])
                   }

                   model Child{
                       id         String  @id @default(cuid())
                       c          String  @unique
                       parentsOpt Parent[]
                   }

                   model StepChild{
                        id        String  @id @default(cuid())
                        s         String  @unique
                        parentOpt Parent?
                   }"""

      val dm2 = """model Parent{
                       id           String     @id @default(cuid())
                       p            String     @unique
                       childrenOpt  Child[]
                       stepChildOpt StepChild? @relation(references: [id])
                   }

                   model Child{
                       id         String @id @default(cuid())
                       c          String @unique
                       parentsOpt Parent[]
                   }

                   model StepChild{
                        id        String  @id @default(cuid())
                        s         String  @unique
                        parentOpt Parent?
                   }"""
      TestDataModels(mongo = dm1, sql = dm2)
    }

    testDataModels.testV11 { project =>
      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"}]
        |    }
        |    stepChildOpt: {
        |      create: {s: "s1"}
        |    }
        |  }){
        |    p
        |  }
        |}""".stripMargin,
        project
      )

      server.query(
        s"""
         |mutation {
         |  deleteManyParents(
         |  where: { p: "p1"}
         | ){
         |  count
         |  }
         |}
      """.stripMargin,
        project
      )
    }
  }
}

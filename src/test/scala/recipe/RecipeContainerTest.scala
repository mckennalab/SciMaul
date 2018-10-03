package recipe


import algorithms.BarcodeEditDistance
import org.scalatest.{FlatSpec, Matchers}


class RecipeContainerTest extends FlatSpec with Matchers {
  "RecipeReader" should "load up an example recipe" in {
    val recipe = new RecipeContainer("test_data/sci_GESTALT_v83.json")

    ((recipe.recipe.name) should be ("SCI-RNA-Seq"))
    ((recipe.recipe.barcodes.size) should be (5))


    ((recipe.recipe.barcodes(0).name) should be ("row"))
    ((recipe.recipe.barcodes(0).read) should be ("INDEX1"))
    ((recipe.recipe.barcodes(0).start) should be (0))
    ((recipe.recipe.barcodes(0).length) should be (10))
    ((recipe.recipe.barcodes(0).use) should be ("INDEX"))
    ((recipe.recipe.barcodes(0).sequences.get) should be ("sci_2_level/SCI_P5.txt"))

    ((recipe.recipe.barcodes(4).name) should be ("static"))
    ((recipe.recipe.barcodes(4).read) should be ("READ1"))
    ((recipe.recipe.barcodes(4).start) should be (58))
    ((recipe.recipe.barcodes(4).length) should be (10))
    ((recipe.recipe.barcodes(4).use) should be ("static"))
    ((recipe.recipe.barcodes(4).sequences.isDefined) should be (false))
  }
}
package plugin

import pumpkin.ItemStack
import pumpkin.Recipe
import pumpkin.Server

/** Returns the server recipe manager. */
fun Server.Server.recipes(): Recipe.RecipeManager = getRecipeManager()

fun Recipe.RecipeManager.shaped(
    id: String,
    output: ItemStack.ItemStack,
    pattern: List<String>,
    key: Map<String, Recipe.Ingredient>,
) {
    registerShaped(
        id,
        Recipe.ShapedRecipe(
            pattern = pattern,
            key = key.toList(),
            output = output,
            group = null,
            category = null,
            showNotification = true,
        ),
    )
}

fun Recipe.RecipeManager.shapeless(
    id: String,
    output: ItemStack.ItemStack,
    ingredients: List<Recipe.Ingredient>,
) {
    registerShapeless(
        id,
        Recipe.ShapelessRecipe(
            ingredients = ingredients,
            output = output,
            group = null,
            category = null,
        ),
    )
}

fun Recipe.RecipeManager.cooking(
    id: String,
    station: Recipe.CookingType,
    ingredient: Recipe.Ingredient,
    output: ItemStack.ItemStack,
    experience: Float = 0.0f,
    cookingTime: UInt = 200u,
) {
    registerCooking(
        id,
        station,
        Recipe.CookingRecipe(
            ingredient = ingredient,
            output = output,
            experience = experience,
            cookingTime = cookingTime,
            group = null,
            category = null,
        ),
    )
}

fun itemIngredient(id: String): Recipe.Ingredient = Recipe.Ingredient.Item(id)

fun tagIngredient(tag: String): Recipe.Ingredient = Recipe.Ingredient.Tag(tag)

fun oneOfIngredient(ids: List<String>): Recipe.Ingredient = Recipe.Ingredient.OneOf(ids)

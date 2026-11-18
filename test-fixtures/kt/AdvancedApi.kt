package pumpkin

// Minimal JVM fixture for the compat modules that are compiled by verify.sh.
// It mirrors the generated API shape, not behaviour.

interface Scoreboard {
    enum class RenderType { INTEGER, HEARTS }
    enum class DisplaySlot { PLAYER_LIST, SIDEBAR, BELOW_NAME }
    enum class NametagVisibility { ALWAYS, NEVER }
    enum class CollisionRule { ALWAYS, NEVER }
    class TeamSettings(
        var displayName: Text.TextComponent,
        var friendlyFire: Boolean,
        var seeFriendlyInvisibles: Boolean,
        var nametagVisibility: NametagVisibility,
        var collisionRule: CollisionRule,
        var color: Common.NamedColor,
        var prefix: Text.TextComponent,
        var suffix: Text.TextComponent,
    )
    class Scoreboard {
        fun addObjective(name: String, displayName: Text.TextComponent, renderType: RenderType, numberFormat: Any?) {}
        fun updateObjective(name: String, displayName: Text.TextComponent, renderType: RenderType, numberFormat: Any?) {}
        fun updateScore(entityName: String, objectiveName: String, value: Int, numberFormat: Any?) {}
        fun setDisplaySlot(slot: DisplaySlot, objectiveName: String) {}
        fun createTeam(name: String, settings: TeamSettings) {}
    }
}

interface Forms {
    enum class ImageType { URL, PATH }
    class FormImage(var type: ImageType, var data: String)
    class SimpleFormButton(var text: Text.TextComponent, var image: FormImage?)
    class SimpleForm(var title: Text.TextComponent, var content: Text.TextComponent, var buttons: List<SimpleFormButton>)
    class ModalForm(
        var title: Text.TextComponent,
        var content: Text.TextComponent,
        var button1: Text.TextComponent,
        var button2: Text.TextComponent,
    )
    sealed interface CustomFormElement {
        class Label(val value: Text.TextComponent) : CustomFormElement
        class Toggle(val value: Pair<Text.TextComponent, Boolean>) : CustomFormElement
        class Input(val value: Triple<Text.TextComponent, String, String>) : CustomFormElement
        class Dropdown(val value: Triple<Text.TextComponent, List<String>, UInt>) : CustomFormElement
        class Slider(val value: pumpkin.runtime.Tuple5<Text.TextComponent, Float, Float, Float, Float>) : CustomFormElement
        class StepSlider(val value: Triple<Text.TextComponent, List<String>, UInt>) : CustomFormElement
    }
    class CustomForm(var title: Text.TextComponent, var elements: List<CustomFormElement>)
    sealed interface Form {
        class Simple(val value: SimpleForm) : Form
        class Modal(val value: ModalForm) : Form
        class Custom(val value: CustomForm) : Form
    }
}

interface Recipe {
    enum class RecipeCategory { BUILDING, REDSTONE, EQUIPMENT, MISC, FOOD, BLOCKS }
    sealed interface Ingredient {
        class Item(val value: String) : Ingredient
        class Tag(val value: String) : Ingredient
        class OneOf(val value: List<String>) : Ingredient
    }
    class ShapedRecipe(
        var pattern: List<String>,
        var key: List<Pair<String, Ingredient>>,
        var output: ItemStack.ItemStack,
        var group: String?,
        var category: RecipeCategory?,
        var showNotification: Boolean?,
    )
    class ShapelessRecipe(
        var ingredients: List<Ingredient>,
        var output: ItemStack.ItemStack,
        var group: String?,
        var category: RecipeCategory?,
    )
    class CookingRecipe(
        var ingredient: Ingredient,
        var output: ItemStack.ItemStack,
        var experience: Float,
        var cookingTime: UInt,
        var group: String?,
        var category: RecipeCategory?,
    )
    enum class CookingType { SMELTING, BLASTING, SMOKING, CAMPFIRE }
    class RecipeManager {
        fun registerShaped(id: String, recipe: ShapedRecipe) {}
        fun registerShapeless(id: String, recipe: ShapelessRecipe) {}
        fun registerCooking(id: String, stationType: CookingType, recipe: CookingRecipe) {}
    }
}

interface Datapack {
    class DatapackInfo(
        var id: String,
        var name: String,
        var description: String,
        var packFormat: UInt,
        var isEnabled: Boolean,
        var recipeCount: UInt,
        var functionCount: UInt,
    )
    sealed interface EnablePosition {
        object First : EnablePosition
        object Last : EnablePosition
        class Before(val value: String) : EnablePosition
        class After(val value: String) : EnablePosition
    }
    class DatapackManager {
        fun listAllPacks(): List<DatapackInfo> = emptyList()
        fun listEnabledPacks(): List<DatapackInfo> = emptyList()
        fun listAvailablePacks(): List<DatapackInfo> = emptyList()
        fun getPack(name: String): DatapackInfo? = null
        fun isEnabled(name: String): Boolean = false
        fun enablePack(name: String, position: EnablePosition): Result<Unit> = Result.success(Unit)
        fun disablePack(name: String): Result<Unit> = Result.success(Unit)
        fun reload(): Result<Unit> = Result.success(Unit)
        fun executeFunction(name: String): Result<UInt> = Result.success(0u)
    }
}

interface Enchantments {
    enum class AttributeModifierSlot { ANY, MAIN_HAND, OFF_HAND, HAND, FEET, LEGS, CHEST, HEAD, ARMOR, BODY, SADDLE }
    enum class Enchantment { SHARPNESS, PROTECTION, EFFICIENCY }
    class CustomEnchantment(
        var id: String,
        var description: Text.TextComponent,
        var maxLevel: UInt,
        var anvilCost: UInt,
        var supportedItems: String,
        var weight: UInt,
        var slots: List<AttributeModifierSlot>,
        var exclusiveSet: List<String>,
    )
    class EnchantmentManager {
        fun registerEnchantment(enchantment: CustomEnchantment): Result<Unit> = Result.success(Unit)
    }
}

interface Display {
    enum class BillboardMode { FIXED, VERTICAL, HORIZONTAL, CENTER }
    enum class ItemDisplayMode { NONE, HEAD, GUI, GROUND, FIXED }
    enum class TextAlignment { CENTER, LEFT, RIGHT }
    class Vector3f(var x: Float, var y: Float, var z: Float)
    class Quaternionf(var x: Float, var y: Float, var z: Float, var w: Float)
    class DisplayTransformation(
        var translation: Vector3f,
        var scale: Vector3f,
        var leftRotation: Quaternionf,
        var rightRotation: Quaternionf,
    )
    open class DisplayEntity {
        fun getEntity(): World.Entity = World.Entity()
        fun getDisplay(): DisplayEntity = this
        fun setTransformation(transformation: DisplayTransformation) {}
        fun setBillboard(mode: BillboardMode) {}
        fun setViewRange(range: Float) {}
        fun setShadowRadius(radius: Float) {}
        fun setShadowStrength(strength: Float) {}
        fun setDisplayWidth(width: Float) {}
        fun setDisplayHeight(height: Float) {}
        fun setGlowColorOverride(color: Int) {}
        fun setBrightness(brightness: Int) {}
    }
    class BlockDisplayEntity : DisplayEntity() {
        fun getBlockStateId(): UShort = 0u
        fun setBlockStateId(stateId: UShort) {}
        companion object {
            fun fromEntity(entity: World.Entity): BlockDisplayEntity? = null
        }
    }
    class ItemDisplayEntity : DisplayEntity() {
        fun setItem(item: ItemStack.ItemStack?) {}
        fun getItem(): ItemStack.ItemStack? = null
        fun setItemDisplayMode(mode: ItemDisplayMode) {}
        companion object {
            fun fromEntity(entity: World.Entity): ItemDisplayEntity? = null
        }
    }
    class TextDisplayEntity : DisplayEntity() {
        fun setText(text: Text.TextComponent) {}
        fun getText(): Text.TextComponent = Text.TextComponent.text("")
        fun setLineWidth(width: Int) {}
        fun setBackground(color: Int) {}
        fun getTextOpacity(): Byte = -1
        fun setTextOpacity(opacity: Byte) {}
        fun setShadow(shadow: Boolean) {}
        fun setSeeThrough(seeThrough: Boolean) {}
        fun setDefaultBackground(defaultBackground: Boolean) {}
        fun setAlignment(alignment: TextAlignment) {}
        companion object {
            fun fromEntity(entity: World.Entity): TextDisplayEntity? = null
        }
    }
}

interface JavaDialogs {
    enum class DialogType { NOTICE, CONFIRMATION, MULTI_ACTION, DIALOG_LIST, SERVER_LINKS }
    enum class AfterAction { PEEK, POP }
    enum class LinkType {
        BUG_REPORT, COMMUNITY_GUIDELINES, SUPPORT, STATUS, FEEDBACK,
        COMMUNITY, WEBSITE, FORUMS, NEWS, ANNOUNCEMENTS
    }

    sealed interface DialogBody {
        class PlainMessage(val value: Text.TextComponent) : DialogBody
        class Item(val value: ItemStack.ItemStack) : DialogBody
    }

    class DialogInputBool(var label: Text.TextComponent, var defaultValue: Boolean)
    class DialogInputText(
        var label: Text.TextComponent,
        var placeholder: Text.TextComponent,
        var defaultValue: String,
    )
    class DialogInputNumberRange(
        var label: Text.TextComponent,
        var minValue: Float,
        var maxValue: Float,
        var initialValue: Float,
        var step: Float,
        var labelFormat: String?,
    )
    class DialogInputSingleOption(
        var label: Text.TextComponent,
        var options: List<Text.TextComponent>,
        var initialIndex: UInt,
    )

    sealed interface DialogInput {
        class Bool(val value: DialogInputBool) : DialogInput
        class Text(val value: DialogInputText) : DialogInput
        class NumberRange(val value: DialogInputNumberRange) : DialogInput
        class SingleOption(val value: DialogInputSingleOption) : DialogInput
    }

    class CustomClickAction(var id: String, var payload: List<UByte>?)

    sealed interface Action {
        class OpenUrl(val value: String) : Action
        class CustomClick(val value: CustomClickAction) : Action
    }

    class ActionButton(
        var text: Text.TextComponent,
        var tooltip: Text.TextComponent?,
        var width: UInt?,
        var action: Action,
    )

    sealed interface LinkLabel {
        class BuiltIn(val value: LinkType) : LinkLabel
        class Custom(val value: Text.TextComponent) : LinkLabel
    }

    class Link(var label: LinkLabel, var url: String)

    class Dialog(
        var title: Text.TextComponent,
        var type: DialogType,
        var body: List<DialogBody>,
        var inputs: List<DialogInput>,
        var buttons: List<ActionButton>,
        var links: List<Link>,
        var afterAction: AfterAction?,
        var canCloseWithEscape: Boolean,
        var externalTitle: Text.TextComponent?,
    )
}

interface EntityTypes {
    enum class EntityType { BLOCK_DISPLAY, ITEM_DISPLAY, TEXT_DISPLAY }
}

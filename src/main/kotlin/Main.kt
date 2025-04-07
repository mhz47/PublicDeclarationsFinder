import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty() || args.size > 1) {
        println("Usage: <path-to-library>")
        return
    }

    val sourceFiles = File(args[0]).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    if (sourceFiles.isEmpty()) {
        println("No Kotlin source files found.")
        return
    }

    val disposable = Disposer.newDisposable()
    try {
        val environment =
            KotlinCoreEnvironment.createForProduction(
                disposable,
                CompilerConfiguration().apply {
                    put(
                        MESSAGE_COLLECTOR_KEY,
                        PrintingMessageCollector(System.out, MessageRenderer.PLAIN_FULL_PATHS, false),
                    )
                },
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )
        val psiFactory = KtPsiFactory(environment.project)

        sourceFiles.forEach { file ->
            try {
                val ktFile = parseKotlinFile(file, psiFactory)
                printPublicDeclarations(ktFile)
            } catch (e: Exception) {
                println("Parsing error in file '${file.path}': ${e.message}")
            }
        }
    } finally {
        Disposer.dispose(disposable)
    }
}

private fun parseKotlinFile(
    file: File,
    psiFactory: KtPsiFactory,
): KtFile {
    val content = file.readText()
    if (content.isBlank()) throw IllegalArgumentException("File '${file.name}' is empty.")
    return psiFactory.createFile(file.name, content)
}

private fun printPublicDeclarations(ktFile: KtFile) {
    fun printPublicDeclarationsRecursive(
        declaration: KtDeclaration,
        indent: String,
    ) {
        val declarationText = formatDeclaration(declaration)
        if (declaration is KtClassOrObject) {
            val nestedDeclarations = declaration.declarations.filter { it.isPublic && it !is KtAnonymousInitializer }
            if (nestedDeclarations.isNotEmpty()) {
                println("$indent$declarationText {")
                nestedDeclarations.forEach { printPublicDeclarationsRecursive(it, "$indent    ") }
                println("$indent}")
                return
            }
        }
        println("$indent$declarationText")
    }

    ktFile.declarations.forEach { declaration ->
        if (declaration.isPublic) printPublicDeclarationsRecursive(declaration, "")
    }
}

private fun formatDeclaration(declaration: KtDeclaration): String {
    return when (declaration) {
        is KtEnumEntry -> "enum entry ${declaration.name ?: "[Unnamed]"}"

        is KtProperty ->
            "${if (declaration.isVar) "var" else "val"} " +
                "${declaration.name}${declaration.typeReference?.text?.let { ": $it" } ?: ""}"

        is KtFunction ->
            "fun ${declaration.name}" +
                "(${declaration.valueParameters.joinToString { "${it.name}: ${it.typeReference?.text}" }})"

        is KtClass -> "${if (declaration.isEnum()) "enum " else ""}class ${declaration.name}"

        is KtObjectDeclaration -> "object ${declaration.name}"

        is KtTypeAlias -> "typealias ${declaration.name} = ${declaration.getTypeReference()?.text}"

        is KtParameter ->
            "${if (declaration.isMutable) "var" else "val"} " +
                "${declaration.name}${declaration.typeReference?.text?.let { ": $it" } ?: ""}"

        else -> if (declaration.name != null) "// Unknown declaration: $declaration.name" else ""
    }
}

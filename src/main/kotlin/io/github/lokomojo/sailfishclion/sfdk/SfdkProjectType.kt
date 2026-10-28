package io.github.lokomojo.sailfishclion.sfdk

data class SfdkProjectType(
    val id: String,
    val description: String,
    val builders: List<String>,
    val defaultBuilder: String,
) {
    override fun toString(): String = "$description ($id)"
}

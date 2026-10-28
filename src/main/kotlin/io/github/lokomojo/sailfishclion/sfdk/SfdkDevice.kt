package io.github.lokomojo.sailfishclion.sfdk

data class SfdkDevice(
    val name: String,
    val user: String,
    val host: String,
    val port: Int,
) {
    override fun toString(): String = "$name ($user@$host:$port)"
}

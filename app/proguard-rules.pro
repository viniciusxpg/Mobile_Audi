# Regras adicionais do R8/ProGuard.
# Media3, Compose e AndroidX já incluem suas próprias regras (consumer rules).

# jaudiotagger: partes de capas usam java.awt/javax.imageio, inexistentes no Android e não usadas
# pelo app; e a biblioteca carrega formatos por reflexão.
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-keep class org.jaudiotagger.** { *; }

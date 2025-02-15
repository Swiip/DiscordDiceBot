rootProject.name = "buttonDiceRoller"
include("discord-connector")
include("discord-connector:api")
include("discord-connector:jda")
include("bot")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("lombok", "org.projectlombok:lombok:1.18.30")
            library("micrometer-core", "io.micrometer:micrometer-core:1.12.1")
            library("reactor-core", "io.projectreactor:reactor-core:3.6.1")
            library("guava", "com.google.guava:guava:32.1.3-jre")
            library("junit-jupiter-api", "org.junit.jupiter:junit-jupiter-api:5.10.1")
            library("junit-jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:5.10.1")
            library("assertj-core", "org.assertj:assertj-core:3.24.2")
            library("logback-classic", "ch.qos.logback:logback-classic:1.4.14")
            library("log4j-to-slf4j", "org.apache.logging.log4j:log4j-to-slf4j:2.22.0")
            library("commons-lang3", "org.apache.commons:commons-lang3:3.14.0")
        }
    }
}

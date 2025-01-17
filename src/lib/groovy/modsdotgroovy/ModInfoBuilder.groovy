/*
 * Copyright (c) 2022 GroovyMC
 * SPDX-License-Identifier: MIT
 */

package modsdotgroovy

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import org.apache.groovy.lang.annotation.Incubating

import static groovy.lang.Closure.DELEGATE_FIRST

@Incubating
@CompileStatic
class ModInfoBuilder {
    /**
     * The modId of the mod. This should match the value of your mod's {@literal @}GMod/{@literal @}Mod annotated main class.
     */
    String modId = null

    /**
     * The friendly name of the mod. This is the name that will be displayed in the in-game Mods screen.<br>
     * Defaults to a capitalized version of the modId if omitted/null.
     */
    @Nullable String displayName = null

    /**
     * The version number of the mod - there's a few well known ${} variables usable here or just hardcode it.<br>
     * ${file.jarVersion} will substitute the value of the Implementation-Version as read from the mod's JAR file metadata.<br>
     * See the associated build.gradle script for how to populate this completely automatically during a build.
     */
    String version = null

    /**
     * A URL to query for updates for this mod.<br>
     * See <a href='https://mcforge.readthedocs.io/en/latest/gettingstarted/autoupdate/'>the JSON update specification</a>
     */
    @Nullable String updateJsonUrl = null

    /**
     * A URL for the "homepage" for this mod, displayed in the in-game Mods screen.
     */
    @Nullable String displayUrl = null

    // todo: change type to a File instead of a String
    /**
     * A file name (in the root of the mod JAR) containing a logo for display in the in-game Mods screen.
     */
    @Nullable String logoFile = null

    /**
     * People you give credits for the mod.
     */
    @Nullable String credits = null

    /**
     * Display Test controls the display for your mod in the server connection screen.<br>
     */
    DisplayTest displayTest = DisplayTest.MATCH_VERSION

    /**
     * A multi-line description text for the mod, displayed in the in-game Mods screen. <br>
     */
    String description = ''

    /**
     * The dependencies of the mod
     */
    List<Dependency> dependencies

    /**
     * Dependencies with which the mod cannot load
     */
    List<Dependency> breaks

    /**
     * The custom properties of the mod
     */
    Map properties = [:]

    /**
     * The quilt entrypoints of the mod
     */
    Map entrypoints = [:]

    /**
     * The contributors of the mod, as a map from name to title. Titles are ignored on Forge.
     */
    Map<String, List<String>> contributors = [:]

    /**
     * The authors of the mod. <br>
     * MDT will automatically format them as 'x, y and z' on Forge.
     */
    List<String> authors = []

    /**
     * A list of contacts for the mod, mapping from name to URL. Ignored on Forge.
     */
    Map<String, String> contact = [:]

    String intermediateMappings = 'net.fabricmc:intermediary'

    private Platform platform

    ModInfoBuilder(Platform platform) {
        this.platform = platform
    }

    void propertyMissing(String name, Object value) {
        properties[name] = value
    }

    void dependencies(@DelegatesTo(value = DependenciesBuilder, strategy = DELEGATE_FIRST)
                      @ClosureParams(value = SimpleType, options = 'modsdotgroovy.DependenciesBuilder') final Closure closure) {
        final dependenciesBuilder = new DependenciesBuilder(platform)
        closure.delegate = dependenciesBuilder
        closure.resolveStrategy = DELEGATE_FIRST
        closure.call(dependenciesBuilder)
        this.dependencies = dependenciesBuilder.build()
    }

    void breaks(@DelegatesTo(value = DependenciesBuilder, strategy = DELEGATE_FIRST)
                      @ClosureParams(value = SimpleType, options = 'modsdotgroovy.DependenciesBuilder') final Closure closure) {
        final dependenciesBuilder = new DependenciesBuilder(platform)
        closure.delegate = dependenciesBuilder
        closure.resolveStrategy = DELEGATE_FIRST
        closure.call(dependenciesBuilder)
        this.breaks = dependenciesBuilder.build()
    }

    void setDescription(final String description) {
        this.description = description.stripIndent()
    }

    void setAuthor(final String author) {
        this.authors = [author]
    }

    void author(final String author) {
        this.authors << author
    }

    void contact(final String name, final String location) {
        contact[name] = location
    }

    void contributor(final String person, final String title) {
        contributors.computeIfAbsent(title, {[]}) << person
    }

    void contributors(final Map<String, List<String>> toAdd) {
        toAdd.each {title, people ->
            contributors.computeIfAbsent(title, {[]}).addAll(people)
        }
    }

    void entrypoints(@DelegatesTo(value = EntrypointsBuilder, strategy = DELEGATE_FIRST)
                     @ClosureParams(value = SimpleType, options = 'modsdotgroovy.EntrypointsBuilder') final Closure closure) {
        final entrypointsBuilder = new EntrypointsBuilder()
        closure.delegate = entrypointsBuilder
        closure.resolveStrategy = DELEGATE_FIRST
        closure.call(entrypointsBuilder)
        this.entrypoints = entrypointsBuilder.entrypoints
    }

    ImmutableModInfo build() {
        Objects.requireNonNull(this.modId, 'Missing modId for ModInfo')
        Objects.requireNonNull(this.version, "Missing version for ModInfo with modId \"${this.modId}\"")

        var quiltInfo = new ImmutableQuiltModInfo(this.intermediateMappings, this.breaks, this.contact, this.contributors)

        return new ImmutableModInfo(
                this.modId,
                this.displayName ?: this.modId.capitalize(),
                this.version,
                this.updateJsonUrl,
                this.displayUrl,
                this.logoFile,
                this.credits,
                this.authors,
                this.description,
                this.dependencies,
                this.properties,
                this.entrypoints,
                this.displayTest,
                quiltInfo)
    }
}

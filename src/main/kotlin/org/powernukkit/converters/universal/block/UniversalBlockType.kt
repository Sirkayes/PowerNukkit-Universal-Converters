/*
 * PowerNukkit Universal Worlds & Converters for Minecraft
 *  Copyright (C) 2020  José Roberto de Araújo Júnior
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.powernukkit.converters.universal.block

import org.powernukkit.converters.api.MinecraftEdition
import org.powernukkit.converters.api.NamespacedId
import org.powernukkit.converters.api.block.PlatformBlockType
import org.powernukkit.converters.internal.enumMapOfNonNullsOrEmpty
import org.powernukkit.converters.internal.toMapOfList
import org.powernukkit.converters.universal.UniversalPlatform
import org.powernukkit.converters.universal.definitions.TrueFalseOptional
import org.powernukkit.converters.universal.definitions.model.block.type.ModelBlockType
import org.powernukkit.converters.universal.definitions.model.block.type.ModelExtraBlock
import org.powernukkit.converters.universal.definitions.model.block.type.ModelUsesProperty

/**
 * @author joserobjr
 * @since 2020-10-09
 */
class UniversalBlockType(
    platform: UniversalPlatform,
    id: NamespacedId,
    override val blockProperties: List<UniversalBlockProperty>,
    override val blockEntityType: UniversalBlockEntityType?,
    val optionalBlockProperties: Set<String>,

    val editionId: Map<MinecraftEdition, NamespacedId>,
    val editionBlockProperties: Map<MinecraftEdition, List<UniversalBlockProperty>>,
    val editionBlockEntityType: Map<MinecraftEdition, UniversalBlockEntityType?>,

    val extraBlocks: Map<MinecraftEdition, List<ModelExtraBlock>>
) : PlatformBlockType<UniversalPlatform>(platform, id) {
    override val universalType get() = this

    constructor(platform: UniversalPlatform, model: ModelBlockType) : this(platform, NamespacedId(model.id),
        blockProperties = model.usesProperties.asSequence()
            .filter { it.onUniversal != TrueFalseOptional.FALSE }
            .map { (name) ->
                requireNotNull(platform.blockPropertiesById[name]) {
                    "The block property $name was not found in the Universal platform while loading $model"
                }
            }.toList(),


        blockEntityType = model.usesBlockEntity?.let { (name) ->
            requireNotNull(platform.blockEntityTypesById[name]) {
                "The block entity type $name was not found in the Universal platform while loading $model"
            }
        },


        optionalBlockProperties = model.usesProperties.asSequence()
            .filter { it.onUniversal == TrueFalseOptional.OPTIONAL }
            .map(ModelUsesProperty::named)
            .toSet(),


        editionId = enumMapOfNonNullsOrEmpty(
            model.bedrock?.let { MinecraftEdition.BEDROCK to NamespacedId(it) },
            model.java?.let { MinecraftEdition.JAVA to NamespacedId(it) }
        ),


        editionBlockProperties = model.usesProperties.asSequence()
            .filter { it.onBedrock || it.onJava }
            .flatMap { usesProperty ->
                val universal = requireNotNull(platform.blockPropertiesById[usesProperty.named]) {
                    "The block property ${usesProperty.named} was not found in the Universal platform while loading $model"
                }
                sequenceOf(
                    if (usesProperty.onJava) MinecraftEdition.JAVA to universal else null,
                    if (usesProperty.onBedrock) MinecraftEdition.BEDROCK to universal else null
                ).filterNotNull()
            }.toMapOfList(),


        editionBlockEntityType = model.usesBlockEntity?.let { uses ->
            if (uses.onJava || uses.onBedrock) {
                val universal = checkNotNull(platform.blockEntityTypesById[uses.named])
                enumMapOfNonNullsOrEmpty(
                    if (uses.onJava) MinecraftEdition.JAVA to universal else null,
                    if (uses.onBedrock) MinecraftEdition.BEDROCK to universal else null,
                )
            } else null
        } ?: emptyMap(),


        extraBlocks = model.extraBlocks.asSequence()
            .flatMap { extraBlock ->
                sequenceOf(MinecraftEdition.JAVA, MinecraftEdition.BEDROCK)
                    .filter { it in extraBlock.on.minecraftEditions }
                    .map { it to extraBlock }
            }.toMapOfList()
    )
}

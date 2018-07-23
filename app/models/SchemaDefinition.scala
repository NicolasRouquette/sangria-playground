package models

import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema._

import scala.concurrent.Future

/**
 * Defines a GraphQL schema for the current project
 */
object SchemaDefinition {
  /**
    * Resolves the lists of characters. These resolutions are batched and
    * cached for the duration of a query.
    */
  val characters = Fetcher.caching(
    (ctx: CharacterRepo, ids: Seq[String]) ⇒
      Future.successful(ids.flatMap(id ⇒ ctx.getHuman(id) orElse ctx.getDroid(id))))(HasId(_.id))

  val EpisodeEnum = EnumType(
    "Episode",
    Some("One of the films in the Star Wars Trilogy"),
    List(
      EnumValue("NEWHOPE",
        value = Episode.NEWHOPE,
        description = Some("Released in 1977.")),
      EnumValue("EMPIRE",
        value = Episode.EMPIRE,
        description = Some("Released in 1980.")),
      EnumValue("JEDI",
        value = Episode.JEDI,
        description = Some("Released in 1983."))))

  val Character: InterfaceType[CharacterRepo, Character] =
    InterfaceType(
      "Character",
      "A character in the Star Wars Trilogy",
      () ⇒ fields[CharacterRepo, Character](
        Field("id", StringType,
          Some("The id of the character."),
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the character."),
          resolve = _.value.name),
        Field("friends", ListType(Character),
          Some("The friends of the character, or an empty list if they have none."),
          complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
          resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e ⇒ Some(e)))
      ))

  lazy val Humanoid: InterfaceType[CharacterRepo, Humanoid] =
    InterfaceType(
      name = "Humanoid",
      description = None,
      fieldsFn = () => fields[CharacterRepo, Humanoid](
        Field("id", StringType,
          Some("The id of the humanoid."),
          tags = ProjectionName("_id") :: Nil,
          resolve = ctx => ctx.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the humanoid."),
          resolve = ctx ⇒ Future.successful(ctx.value.name)),
        Field("friends", ListType(Character),
          Some("The friends of the humanoid, or an empty list if they have none."),
          complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
          resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e ⇒ Some(e))),
      ),
      interfaces = List(Character),
      manualPossibleTypes =
        () => List(PrimaryDroid),
      astDirectives = Vector.empty,
      astNodes = Vector.empty)

  val Human =
    ObjectType(
      "Human",
      "A humanoid creature in the Star Wars universe.",
      interfaces[CharacterRepo, Human](Humanoid),
      fields[CharacterRepo, Human](
        Field("id", StringType,
          Some("The id of the human."),
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the human."),
          resolve = _.value.name),
        Field("friends", ListType(Character),
          Some("The friends of the human, or an empty list if they have none."),
          complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
          resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e ⇒ Some(e))),
        Field("homePlanet", OptionType(StringType),
          Some("The home planet of the human, or null if unknown."),
          resolve = _.value.homePlanet)
      ))

  lazy val Droid: InterfaceType[CharacterRepo, Droid] =
    InterfaceType(
    name = "Droid",
    description = Some("A mechanical creature in the Star Wars universe."),
    fieldsFn = () => fields[CharacterRepo, Droid](
        Field("id", StringType,
        Some("The id of the droid."),
        tags = ProjectionName("_id") :: Nil,
        resolve = ctx => ctx.value.id),
      Field("name", OptionType(StringType),
        Some("The name of the droid."),
        resolve = ctx ⇒ Future.successful(ctx.value.name)),
      Field("friends", ListType(Character),
        Some("The friends of the droid, or an empty list if they have none."),
        complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
        resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
      Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
        Some("Which movies they appear in."),
        resolve = ctx => ctx.value.appearsIn map (e ⇒ Some(e))),
      Field("primaryFunction", OptionType(StringType),
        Some("The primary function of the droid."),
        resolve = ctx => ctx.value.primaryFunction),
      Field("follows", OptionType(Character),
        Some("The character this droid follows"),
        resolve = ctx => ctx.value.follows)
    ),
      interfaces = List(Character),
      manualPossibleTypes =
        () => List(PrimaryDroid, SecondaryDroid),
      astDirectives = Vector.empty,
      astNodes = Vector.empty)

  lazy val HumanoDroid: InterfaceType[CharacterRepo, HumanoDroid] =
    InterfaceType(
      name = "HumanoDroid",
      description = None,
      fieldsFn = () => fields[CharacterRepo, HumanoDroid](
        Field("id", StringType,
          Some("The id of the droid."),
          tags = ProjectionName("_id") :: Nil,
          resolve = ctx => ctx.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the droid."),
          resolve = ctx ⇒ Future.successful(ctx.value.name)),
        Field("friends", ListType(Character),
          Some("The friends of the droid, or an empty list if they have none."),
          complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
          resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = ctx => ctx.value.appearsIn map (e ⇒ Some(e))),
        Field("primaryFunction", OptionType(StringType),
          Some("The primary function of the droid."),
          resolve = ctx => ctx.value.primaryFunction),
        Field("follows", OptionType(Humanoid),
          Some("The character this droid follows"),
          resolve = ctx => ctx.value.follows)
      ),
      interfaces = List(Droid),
      manualPossibleTypes =
        () => List(PrimaryDroid),
      astDirectives = Vector.empty,
      astNodes = Vector.empty)

  lazy val MonoDroid: InterfaceType[CharacterRepo, MonoDroid] =
    InterfaceType(
      name = "MonoDroid",
      description = None,
      fieldsFn = () => fields[CharacterRepo, MonoDroid](
        Field("id", StringType,
          Some("The id of the droid."),
          tags = ProjectionName("_id") :: Nil,
          resolve = ctx => ctx.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the droid."),
          resolve = ctx ⇒ Future.successful(ctx.value.name)),
        Field("friends", ListType(Character),
          Some("The friends of the droid, or an empty list if they have none."),
          complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
          resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = ctx => ctx.value.appearsIn map (e ⇒ Some(e))),
        Field("primaryFunction", OptionType(StringType),
          Some("The primary function of the droid."),
          resolve = ctx => ctx.value.primaryFunction),
        Field("follows", OptionType(Droid),
          Some("The character this droid follows"),
          resolve = ctx => ctx.value.follows)
      ),
      interfaces = List(Droid),
      manualPossibleTypes =
        () => List(SecondaryDroid),
      astDirectives = Vector.empty,
      astNodes = Vector.empty)

  lazy val PrimaryDroid = ObjectType(
    "PrimaryDroid",
    "A droid who follows a humanoid",
    interfaces[CharacterRepo, PrimaryDroid](HumanoDroid),
    fields[CharacterRepo, PrimaryDroid](
      Field("id", StringType,
        Some("The id of the droid."),
        tags = ProjectionName("_id") :: Nil,
        resolve = _.value.id),
      Field("name", OptionType(StringType),
        Some("The name of the droid."),
        resolve = ctx ⇒ Future.successful(ctx.value.name)),
      Field("friends", ListType(Character),
        Some("The friends of the droid, or an empty list if they have none."),
        complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
        resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
      Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
        Some("Which movies they appear in."),
        resolve = _.value.appearsIn map (e ⇒ Some(e))),
      Field("primaryFunction", OptionType(StringType),
        Some("The primary function of the droid."),
        resolve = _.value.primaryFunction),
      Field("follows", OptionType(Humanoid),
        None,
        resolve = _.value.follows)
    ))

  lazy val SecondaryDroid = ObjectType(
    "SecondaryDroid",
    "A droid who follows another droid",
    interfaces[CharacterRepo, SecondaryDroid](MonoDroid),
    fields[CharacterRepo, SecondaryDroid](
      Field("id", StringType,
        Some("The id of the droid."),
        tags = ProjectionName("_id") :: Nil,
        resolve = _.value.id),
      Field("name", OptionType(StringType),
        Some("The name of the droid."),
        resolve = ctx ⇒ Future.successful(ctx.value.name)),
      Field("friends", ListType(Character),
        Some("The friends of the droid, or an empty list if they have none."),
        complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
        resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
      Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
        Some("Which movies they appear in."),
        resolve = _.value.appearsIn map (e ⇒ Some(e))),
      Field("primaryFunction", OptionType(StringType),
        Some("The primary function of the droid."),
        resolve = _.value.primaryFunction),
      Field("follows", OptionType(Droid),
        None,
        resolve = _.value.follows)
    ))

  val ID = Argument("id", StringType, description = "id of the character")

  val EpisodeArg = Argument("episode", OptionInputType(EpisodeEnum),
    description = "If omitted, returns the hero of the whole saga. If provided, returns the hero of that particular episode.")

  val Query = ObjectType(
    "Query", fields[CharacterRepo, Unit](
      Field("hero", Character,
        arguments = EpisodeArg :: Nil,
        deprecationReason = Some("Use `human` or `droid` fields instead"),
        resolve = (ctx) ⇒ ctx.ctx.getHero(ctx.arg(EpisodeArg))),
      Field("human", OptionType(Human),
        arguments = ID :: Nil,
        resolve = ctx ⇒ ctx.ctx.getHuman(ctx arg ID)),
      Field("droid", Droid,
        arguments = ID :: Nil,
        resolve = Projector((ctx, f) ⇒ ctx.ctx.getDroid(ctx arg ID).get))
    ))

  val StarWarsSchema = Schema(Query)
}

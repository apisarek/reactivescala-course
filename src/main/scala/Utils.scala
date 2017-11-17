import java.util.UUID

object Utils {
  def generateID(): String = UUID.randomUUID().toString
}

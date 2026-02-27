package libcurl

import scala.scalanative.meta.LinktimeInfo.isWindows
import scala.scalanative.unsafe.{extern, name, link, define, CString}

object functions:

  @extern @link("libcurl") @link("libssl") @link("libcrypto")
  private object CurlFunctionsWindows extends functions

  @extern @link("curl")
  private object CurlFunctionsUnix extends functions

  val _functions = if isWindows then CurlFunctionsWindows else CurlFunctionsUnix

  export _functions.*

end functions

@define("CURL_NO_OLDIES") // deprecate all outdated
@extern
trait functions:

  /** <curl/curl.h>
    */

  /** NAME curl_version()
    *
    * DESCRIPTION
    *
    * Returns a static ascii string of the libcurl version.
    */
  @name("curl_version")
  def curlVersion(): CString = extern

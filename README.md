# SecureGpxParser

A Gpx-parser which allows validation of waypoints by SHA-224 hashes. Each waypoint's hash is calculated out of the waypoint' properties (coordinates, date and accuracy), the hash value of the previous waypoint and a secret String provided by the method WayPoint#getHashSecretKey() which should be overwritten by poductive implementations.

## How the validation works
GPX Files can be edited by the use of the methods SecureGpxParser#addTrackpoint(...), SecureGpxParser#removeLocation or by direclty modifiing existin Waypoints provided by SecureGpxParser#getLocations(). These methods will generate valid hash values and with SecureGpxParser#save() they can be written to a GPX file. To validate unknown files (geneated by the use of this library and with same secret key), the parser can be initialized by the static method SecureGpxParser#fromFile(File file). The obtained SecureGpxParser can be validated by the method SecureGpxParser#requestValidation(GPXValidationListener listener) which will perform the validation on a background thread and pass the result to the listener.



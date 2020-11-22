# SecureGpxParser

A Gpx-parser which allows validation of waypoints by SHA-224 hashes. Each waypoint's hash is calculated out of the waypoint' properties (coordinates, date and accuracy), the hash value of the previous waypoint and a secret string provided by the method WayPoint#getHashSecretKey() which should be overwritten by poductive implementations.

## How the validation works
GPX Files can be edited by the use of the methods SecureGpxParser#addTrackpoint(...), SecureGpxParser#removeLocation or by direclty modifiing existin Waypoints provided by SecureGpxParser#getLocations(). These methods will generate valid hash values and with SecureGpxParser#save() they can be written to a GPX file. To validate unknown files (geneated by the use of this library and with same secret key), the parser can be initialized by the static method SecureGpxParser#fromFile(File file). The obtained SecureGpxParser can be validated by the method SecureGpxParser#requestValidation(GPXValidationListener listener) which will perform the validation on a background thread and pass the result to the listener.


## Are the produced GPX-files blockchains?
According to [Wikipedia](https://wikipedia.org/wiki/Blockchain) a blockchain should:
* consist of blocks with a secure hash value -> A block is represented by a Waypoint
* the blocks are connected to each other via their hash values -> ok
* expandable -> ok
* distributed storage

All points except for the last one are fulfilled. The distributed storage could be implemented easily by adding a / multiple Hashstores backend (e.g. with Mongo-DB as database). This Hashstore should contain all Hashvalues. In case of validation, the last hash value would be calculated locally and by finding the last hash value in the strore we have validated the chain.
To obtain a secure validation process without any backend, the library uses a secrect String to create the values of each Waypoint. It is very important, that productive secret keys are not known to others! Without knowing this secret key, it is not possible to fake a gpx-file.

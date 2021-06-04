package life.qbic.samplestatusupdater.update

import groovy.util.logging.Log4j2
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import life.qbic.datamodel.samples.Location
import life.qbic.samplestatusupdater.ServiceUserCredentials
import life.qbic.services.Service

@Log4j2
class SampleTrackingServiceConnector implements SampleTrackingService {

    ServiceUserCredentials credentials

    Service service

    SampleTrackingServiceConnector(Service sampleTrackingService, ServiceUserCredentials credentials){
        this.service = sampleTrackingService
        this.credentials = credentials
    }

    private def updateSampleLocation(String sampleCode, Location location) {
        HttpClient client = RxHttpClient.create(service.rootUrl)
        //TODO this is only a workaround, as the client seems not to prepend the base url.
        URI sampleUri = new URI("${service.rootUrl.toExternalForm()}/samples/$sampleCode/currentLocation/")

        HttpRequest request = HttpRequest.POST(sampleUri, location).basicAuth(credentials.name, credentials.pw)
        client.withCloseable {
            it.toBlocking().exchange(request)
        }
    }

    @Override
    def registerFirstSampleLocation(String sampleCode, Location location) throws SampleUpdateException {
        HttpClient client = RxHttpClient.create(service.rootUrl)
        //TODO this is only a workaround, as the client seems not to prepend the base url.
        URI sampleUri = new URI("${service.rootUrl.toExternalForm()}/samples/$sampleCode/")

        HttpRequest request = HttpRequest.GET(sampleUri).basicAuth(credentials.name, credentials.pw)

        /*
        If the sample already is registered in the service, we do not want to change its location
         */
        try {
            def response = client.withCloseable { it.toBlocking().exchange(request) }
        } catch (HttpClientResponseException e) {
            if (e.response.status == HttpStatus.NOT_FOUND) {
                log.info "Sample $sampleCode not yet registered, setting first sample location QBiC..."
                updateSampleLocation(sampleCode, location)
            } else {
                log.error("Http response exception: ${e.message}, ${e.response}")
                throw new SampleUpdateException("Could not update sample ${sampleCode}")
            }
        }

    }


}

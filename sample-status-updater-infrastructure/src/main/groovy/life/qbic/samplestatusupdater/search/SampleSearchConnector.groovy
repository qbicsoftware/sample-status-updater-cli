package life.qbic.samplestatusupdater.search

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi

class SampleSearchConnector implements OpenBisSearchService {

    IApplicationServerApi apiConnection

    String token

    SampleSearchConnector(IApplicationServerApi apiConnection, String token) {
        this.apiConnection = apiConnection
        this.token = token
    }

    @Override
    List<String> findNewOpenBisSamplesSince(Date registeredSince, List sampleTypeFilter) {
        def criteria = new SampleSearchCriteria()
        criteria.withRegistrationDate().thatIsLaterThanOrEqualTo(registeredSince)

        def fetchOptions = new SampleFetchOptions()
        fetchOptions.withType()

        def searchResult = apiConnection.searchSamples(token, criteria, fetchOptions)
        def sampleList = searchResult.getObjects().findAll {
            sampleTypeFilter.contains(it.type.code)
        }
        return  sampleList.collect { it.code }
    }
}

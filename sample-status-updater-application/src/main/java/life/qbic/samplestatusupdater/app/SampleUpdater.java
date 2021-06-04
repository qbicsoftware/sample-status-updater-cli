package life.qbic.samplestatusupdater.app;

import life.qbic.cli.QBiCTool;
import life.qbic.samplestatusupdater.OpenBisSession;
import life.qbic.samplestatusupdater.ServiceSearch;
import life.qbic.samplestatusupdater.ServiceUserCredentials;
import life.qbic.samplestatusupdater.UseCaseConnector;
import life.qbic.samplestatusupdater.cli.ApplicationProperties;
import life.qbic.samplestatusupdater.cli.SampleUpdatePresenter;
import life.qbic.samplestatusupdater.search.OpenBisSearchService;
import life.qbic.samplestatusupdater.search.SampleSearchConnector;
import life.qbic.samplestatusupdater.search.FindNewOpenBisSamples;
import life.qbic.samplestatusupdater.search.SampleTypeFilter;
import life.qbic.samplestatusupdater.update.SampleTrackingService;
import life.qbic.samplestatusupdater.update.SampleTrackingServiceConnector;
import life.qbic.samplestatusupdater.update.UpdateSampleStatus;
import life.qbic.samplestatusupdater.update.UpdateSampleStatusImpl;
import life.qbic.services.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Sample command-line Tool. Its command-line arguments are contained in instances of {@link StatusUpdaterCommand}.
 */
public class SampleUpdater extends QBiCTool<StatusUpdaterCommand> {

    private static final Logger LOG = LogManager.getLogger(SampleUpdater.class);

    /**
     * Constructor.
     *
     * @param command an object that represents the parsed command-line arguments.
     */
    public SampleUpdater(final StatusUpdaterCommand command) {
        super(command);
    }

    @Override
    public void execute() {
        // get the parsed command-line arguments
        final StatusUpdaterCommand command = super.getCommand();

        ApplicationProperties appProperties = new ApplicationProperties(command.config);
        Map properties = (Map) appProperties.parse();
        OpenBisSession session = new OpenBisSession(
                                        (String) properties.get("openbisAsUrl"),
                                        (String) properties.get("openbisUser"),
                                        (String) properties.get("openbisPw"));
        ServiceUserCredentials credentials = new ServiceUserCredentials(
                                                    (String) properties.get("serviceUser"),
                                                    (String) properties.get("serviceUserPw"));
        URL serviceRegistry = createUrlFromString((String) properties.get("serviceRegistryUrl"));

        List<Service> serviceList = ServiceSearch.findSampleTrackingService(serviceRegistry);

        if (serviceList.isEmpty()) {
            LOG.error("Could not find any services for sample tracking!");
            System.exit(1);
        } else {
            LOG.info("Found at least one sample tracking service.");
            LOG.debug("Service address: " + serviceList.get(0).getRootUrl());
        }

        SampleTrackingService sampleTrackingService = new SampleTrackingServiceConnector(serviceList.get(0), credentials);

        OpenBisSearchService searchService = new SampleSearchConnector(session.getApi(), session.getToken());
        Date lastSearchDate = parseDateFromStringWithPattern((String) properties.get("lastSearchDate"), "yyyy-MM-dd");

        SampleUpdatePresenter updatePresenter = new SampleUpdatePresenter();
        int exitCode = 0;
        try {
            final UpdateSampleStatus updateSampleStatus = new UpdateSampleStatusImpl(sampleTrackingService, updatePresenter);
            UseCaseConnector useCaseConnector = new UseCaseConnector(updateSampleStatus);
            final FindNewOpenBisSamples findNewOpenBisSamples = new FindNewOpenBisSamples(searchService, useCaseConnector);
            findNewOpenBisSamples.searchNewSamplesSince(lastSearchDate);
        } catch (Exception e) {
            exitCode = 1;
        }
        properties.put("lastSearchDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        appProperties.updatePropertyFile(properties);

        System.exit(exitCode);
    }

    URL createUrlFromString(String urlString){
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            LOG.error("URL for service registry was malformatted.", e);
        }
        return url;
    }

    Date parseDateFromStringWithPattern(String date, String pattern){
        Date lastSearchDate = null;
        try {
            lastSearchDate = new SimpleDateFormat(pattern).parse(date);
        } catch (ParseException e) {
            LOG.error("Could not parse date from date String " + date);
        }
        return lastSearchDate;
    }

    // TODO: override the shutdown() method if you are implementing a daemon and want to take advantage of a shutdown hook for clean-up tasks
}
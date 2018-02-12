package de.gunis.roger.icalToGoogleDeployment;


import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Lists;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.google.api.services.calendar.model.Calendar;
import javafx.util.Pair;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.abs;

public class IcsFilesToGoogleCalendarDeployment {
    private static final Logger logger = LoggerFactory.getLogger(IcsFilesToGoogleCalendarDeployment.class);

    /**
     * Be sure to specify the name of your application. If the application name is {@code null} or
     * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
     */
    private static final String APPLICATION_NAME = "FishFingers-Calendars/1.0";

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(names = {"--help", "-h"}, description = "This help", help = true)
    private static boolean help = false;

    @SuppressWarnings("unused FieldCanBeLocal")
    @Parameter(names = {"--loggingLevel", "-log"}, description = "Level of verbosity [ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF]")
    private String loggingLevel;

    @SuppressWarnings("unused FieldCanBeLocal")
    @Parameter(required = true, names = {"--icsDirectory", "-ics"}, description = "Directory for ics files>")
    private String inputDirectoryIcsFiles;

    @SuppressWarnings("unused FieldCanBeLocal")
    @Parameter(required = true, names = {"--apiKey", "-api"}, description = "the api key generated through " +
            "https://console.developers.google.com/iam-admin/serviceaccounts -> Dienstkonten")
    private String apiKeyFile;

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static com.google.api.services.calendar.Calendar client;

    private static final java.util.List<Event> addedEventsUsingBatch = Lists.newArrayList();
    private static final String CALENDAR_SUFFIX = ".ics";

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private static GoogleCredential authorize(InputStream apiKeyFile) throws Exception {
        String accountUser = System.getProperty("ACCOUNT_USER");
        if (StringUtils.isBlank(accountUser)) {
            throw new GeneralSecurityException("ACCOUNT_USER environment property is missing");
        }


        List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
        GoogleCredential cr = GoogleCredential
                .fromStream(apiKeyFile)
                .createScoped(SCOPES);

        GoogleCredential.Builder builder = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountScopes(SCOPES)
                .setServiceAccountId(cr.getServiceAccountId())
                .setServiceAccountPrivateKey(cr.getServiceAccountPrivateKey())
                .setServiceAccountPrivateKeyId(cr.getServiceAccountPrivateKeyId())
                .setTokenServerEncodedUrl(cr.getTokenServerEncodedUrl())
                .setServiceAccountUser(accountUser);
        return builder.build();

    }

    public static void main(String[] args) {

        IcsFilesToGoogleCalendarDeployment main = new IcsFilesToGoogleCalendarDeployment();
        JCommander jCommander = new JCommander(main, args);

        if (help) {
            jCommander.usage();
            logger.info("-------------\nAll elements marked with * are required\n-------------\n");
        } else {
            main.runDeployment();
        }

    }


    void runDeployment() {
        setLoggingToLevel(loggingLevel);

        try {
            // initialize the transport
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // authorization
            Credential credential = authorize(new FileInputStream(apiKeyFile));


            // set up global Calendar instance
            client = new com.google.api.services.calendar.Calendar.Builder(
                    httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();


            Acl acl = client.acl().list("primary").execute();

            for (AclRule rule : acl.getItems()) {
                System.out.println(rule.getId() + ": " + rule.getRole());
            }


            Set<String> googleCalendars = getGoogleCalendars();

            Set<String> foundCalendarsInDirectory = getBasenameOfFiles(inputDirectoryIcsFiles);

            Set<String> obsoleteCalendars = googleCalendars.stream()
                    .filter(cal -> !foundCalendarsInDirectory.contains(cal)).collect(Collectors.toSet());

            deployIcsFilesToGoogleCalendar(inputDirectoryIcsFiles, foundCalendarsInDirectory);

            obsoleteCalendars.forEach(IcsFilesToGoogleCalendarDeployment::deleteCalendar);

            Set<String> usedCalendars = googleCalendars.stream()
                    .filter(foundCalendarsInDirectory::contains).collect(Collectors.toSet());


            String accessOfUserStringList = System.getProperty("ACCESS_OF_USERS", "");
            Set<String> accessOfUsers = Arrays.stream(accessOfUserStringList.split("\\s*,\\s*")).collect(Collectors.toSet());

            usedCalendars.forEach(cal -> {
                try {
                    // Insert new access rule

                    Calendar calendar = createAndGetCalendar(cal);

//                    AclRule createdRule = client.acl().insert(calendar.getId(), rule).execute();
//                    client.acl().get(calendar.getId(), ruleIdToCheck);
//                    System.out.println("Acl ID: " + createdRule.getId());

                    accessOfUsers.stream()
                            .filter(accessOfUser -> !hasPermissionOnCalendar(calendar, accessOfUser))
                            .forEach(accessOfUser -> {
                                try {
                                    logger.debug("Granting new access");
                                    client.acl().insert(calendar.getId(), getAclRule(accessOfUser)).execute();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
//                    List<AclRule> aclRules = client.acl().list(calendar.getId()).execute().getItems();
//                    Set<String> usersAccessToCalendar = aclRules.stream().map(aclRule -> aclRule.getId().split(":")[1]).collect(Collectors.toSet());

//                    accessOfUsers.stream()
//                            .filter(accessOfUser -> !hasPermissionOnCalendar(calendar, accessOfUser))
//                            .forEach(accessOfUser -> {
//                                try {
//                                    logger.trace("Granting new access");
//                                    client.acl().insert(calendar.getId(), getAclRule(accessOfUser)).execute();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            });

//                    System.out.println("Calendar: " + calendar.getSummary());
//                    System.out.println("https://calendar.google.com/calendar/render?cid=https://calendar.google.com/calendar/ical/" + calendar.getId() + "/public/basic.ics");
//                    System.out.println("webcal://calendar.google.com/calendar/ical/" + calendar.getId() + "/public/basic.ics");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        } catch (IOException e) {
            logger.error("Exception occured");
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private AclRule getAclRule(String accessOfUsers) {
        // Create access rule with associated scope
        AclRule rule = new AclRule();
        AclRule.Scope scope = new AclRule.Scope();
        scope.setType("user").setValue(accessOfUsers);
        rule.setScope(scope).setRole("owner");
//        String ruleIdToCheck = rule.getScope().getType().concat(":" + rule.getScope().getValue());
        return rule;
    }

    private boolean hasPermissionOnCalendar(Calendar calendar, String accessOfUser) {
        Acl acl2;
        try {
            acl2 = client.acl().list(calendar.getId()).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return acl2.getItems().stream().anyMatch(aclRule -> aclRule.getId().equals(accessOfUser));
    }


    private static Calendar createAndGetCalendar(String calendarTitle) throws IOException {
        Calendar entry = new Calendar();
        entry.setSummary(calendarTitle);
        List<CalendarListEntry> calendars = client.calendarList().list().execute().getItems().stream()
                .filter(cal -> cal.getSummary().equals(calendarTitle)).collect(Collectors.toList());

        if (calendars.isEmpty()) {
            return client.calendars().insert(entry).execute().setTimeZone("Europe/Berlin");
        } else {
            if (calendars.size() > 1) {
                logger.warn("More than on calendar with name {}, please delete all except one manually", calendarTitle);
            }
            Calendar googleCalendar = client.calendars().get(calendars.get(0).getId()).execute();
            return googleCalendar.setTimeZone("Europe/Berlin");
        }

    }


    private static void deployIcsFilesToGoogleCalendar(String inputDirectoryIcsFiles, Set<String> foundCalendarsInFilesystem) {


        foundCalendarsInFilesystem
                .forEach(calendarTitle -> {
                    try {
                        BatchRequest batch = client.batch();
                        Calendar googleCalendar = createAndGetCalendar(calendarTitle);

                        Pair<HashMap<GoogleEventKey, Event>, HashMap<String, Event>> un_modifiedEvents = getUn_ModifiedEvents(googleCalendar);
                        HashMap<GoogleEventKey, Event> unmodifiedEvents = un_modifiedEvents.getKey();
                        HashMap<String, Event> modifiedEvents = un_modifiedEvents.getValue();

                        FileInputStream fileInputStream = new FileInputStream(inputDirectoryIcsFiles + "/" + calendarTitle.concat(CALENDAR_SUFFIX));
                        CalendarBuilder builder = new CalendarBuilder();
                        net.fortuna.ical4j.model.Calendar calendarOfFile = builder.build(fileInputStream);
                        ComponentList<CalendarComponent> icalEvents = calendarOfFile.getComponents("VEVENT");

                        icalEvents.forEach(icalEvent -> {

                            Event googleEvent = getConvertedIcalEvent(icalEvent);

                            try {
                                if (unmodifiedEvents.containsKey(GoogleEventKey.of(googleEvent.getStart(), googleEvent.getEnd(), googleEvent.getSummary()))) {
                                    logger.debug("Already in googleEvent: " + getEventInformation(googleCalendar, googleEvent));
                                    unmodifiedEvents.remove(GoogleEventKey.of(googleEvent.getStart(), googleEvent.getEnd(), googleEvent.getSummary()));
                                } else if (modifiedEvents.containsKey(getCalculatedEventUid(googleEvent))) {
                                    logger.warn("Modification found in " + calendarTitle + ", googleEvent: " + getEventInformation(googleCalendar, googleEvent));
                                } else {
                                    logger.debug("Putting in googleEvent: " + getEventInformation(googleCalendar, googleEvent));
                                    client.events().calendarImport(googleCalendar.getId(), googleEvent).queue(batch, getEventJsonBatchCallback(googleCalendar, googleEvent));
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }

                        });
                        deleteEventsUsingBatch(googleCalendar, unmodifiedEvents);

                        int numberOfAddEvents = batch.size();
                        if (numberOfAddEvents > 0) {
                            batch.execute();
                            logger.info("adding: #" + numberOfAddEvents + " events in calendar " + googleCalendar.getSummary());
                        } else {
                            logger.info("No add events in calendar " + googleCalendar.getSummary() + ", skipping deployment");
                        }
                    } catch (ParserException | IOException e) {
                        e.printStackTrace();
                    }

                });

    }

    private static Set<String> getBasenameOfFiles(String icsDirectory) throws IOException {
        return Files.list(Paths.get(icsDirectory))
                .filter(path -> path.toString().endsWith(CALENDAR_SUFFIX))
                .map(filename -> filename.toString().split(".+?/(?=[^/]+$)")[1].split("\\.(?=[^.]+$)")[0])
                .collect(Collectors.toSet());
    }

    private static Event getConvertedIcalEvent(CalendarComponent vevent) {
        Event event = new Event();
        event.setSummary(vevent.getProperty(Property.SUMMARY).getValue());
        event.setICalUID(vevent.getProperty(Property.UID).getValue());

        Date dtStamp = ((DtStamp) vevent.getProperty(Property.DTSTAMP)).getDate();
        event.setCreated(new DateTime(dtStamp.getTime()));

        LocalDateTime eventStartDate = LocalDateTime.ofEpochSecond(((DtStart) vevent.getProperty(Property.DTSTART)).getDate().getTime() / 1000, 0, ZoneOffset.ofHours(0));
        event.setStart(new EventDateTime().setDate(new DateTime(true, eventStartDate.toEpochSecond(ZoneOffset.ofHours(0)) * 1000, 0)));

        long durationInDays = ((Duration) vevent.getProperty(Property.DURATION)).getDuration().getDays();
        if (durationInDays <= 1) {
            durationInDays = 0;
        }
        LocalDateTime endDateCalculation = eventStartDate.plusDays(durationInDays);
        event.setEnd(new EventDateTime().setDate(new DateTime(true, endDateCalculation.toEpochSecond(ZoneOffset.ofHours(0)) * 1000, 0)));

        List<EventReminder> listEventReminder = new ArrayList<>();
        Event.Reminders reminders = new Event.Reminders();

        ((VEvent) vevent).getAlarms().forEach(alarms -> alarms.getProperties("Trigger").forEach(clockAlarm -> {
            Duration duration = new Duration(new Dur(clockAlarm.getValue()));

            EventReminder reminder = new EventReminder();
            int minutesOfAlarm = duration.getDuration().getDays() * 24 * 60
                    + duration.getDuration().getHours() * 60
                    + duration.getDuration().getMinutes()
                    + duration.getDuration().getWeeks() * 10080;

            minutesOfAlarm = abs(60 * 24 - minutesOfAlarm);

            reminder.setMinutes(minutesOfAlarm);
            reminder.setMethod("email");

            reminders.setUseDefault(false);
            listEventReminder.add(reminder);
        }));

        reminders.setOverrides(listEventReminder);
        event.setReminders(reminders);
        event.setICalUID(vevent.getProperty(Property.UID).getValue());
        return event;
    }

    private static JsonBatchCallback<Event> getEventJsonBatchCallback(Calendar googleCalendar, Event event) {
        return new JsonBatchCallback<Event>() {
            Event myEvent = event;

            @Override
            public void onSuccess(Event event, HttpHeaders responseHeaders) {
                addedEventsUsingBatch.add(event);
            }

            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                logger.error("Error Message: " + e.getMessage() + ",\n event: " + getEventInformation(googleCalendar, myEvent));
            }
        };
    }

    private static void deleteEventsUsingBatch(Calendar googleCalendar, HashMap<GoogleEventKey, Event> unmodifiedEvents) throws IOException {
        BatchRequest batch = client.batch();
        unmodifiedEvents.forEach((googleEventKey, event) -> {
            try {
                client.events().delete(googleCalendar.getId(), event.getId()).queue(batch, getCallback(googleCalendar, event));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });

        int nrOfEntriesToDelete = batch.size();
        if (nrOfEntriesToDelete > 0) {
            batch.execute();
            logger.info("delete: #" + nrOfEntriesToDelete + " events in calendar " + googleCalendar.getSummary());
        } else {
            logger.info("No delete events in calendar " + googleCalendar.getSummary() + ", skipping deployment");
        }
    }

    private static Pair<HashMap<GoogleEventKey, Event>, HashMap<String, Event>> getUn_ModifiedEvents(Calendar googleCalendar) throws IOException {

        Events googleEvents = client.events().list(googleCalendar.getId()).execute();
        HashMap<GoogleEventKey, Event> unmodifiedEvents = new HashMap<>();
        HashMap<String, Event> modifiedEvents = new HashMap<>();
        googleEvents.getItems().forEach(googleEvent -> {

            String calculatedUID = getCalculatedEventUid(googleEvent);
            String googleUID = googleEvent.getICalUID();

            if (calculatedUID.equals(googleUID)) {
                unmodifiedEvents.put(GoogleEventKey.of(googleEvent.getStart(), googleEvent.getEnd(), googleEvent.getSummary()), googleEvent);
                logger.debug("Calendar: " + googleCalendar.getSummary() + ", Calculated UID: " + calculatedUID + " unmodified event found");
            } else {
                // googleUID taken because this is the origin date
                modifiedEvents.put(googleUID, googleEvent);
                logger.warn("Modification found, Calendar: " + googleCalendar.getSummary() + ", Calculated UID: " + calculatedUID + ", IcalUID: " + googleUID);
            }
        });
        return new Pair<>(unmodifiedEvents, modifiedEvents);
    }

    private static String getEventInformation(Calendar googleCalendar, Event googleEvent) {
        return googleCalendar.getSummary() + ": " + getCalculatedEventUid(googleEvent);
    }

    private static String getCalculatedEventUid(Event googleEvent) {
        return googleEvent.getStart().getDate().toStringRfc3339().replaceAll("-", "").concat("_").concat(googleEvent.getSummary());
    }

    private static JsonBatchCallback<Void> getCallback(Calendar googleCalendar, Event event) {
        return new JsonBatchCallback<Void>() {
            Event myEvent = event;

            @Override
            public void onSuccess(Void content, HttpHeaders responseHeaders) {
                logger.debug("Delete successful: " + getEventInformation(googleCalendar, myEvent));
            }

            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                logger.error("Error Message: " + e.getMessage() + ",\n event: " + getEventInformation(googleCalendar, myEvent));
            }
        };
    }

    public void setInputDirectoryIcsFiles(String inputDirectoryIcsFiles) {
        this.inputDirectoryIcsFiles = inputDirectoryIcsFiles;
    }

    public void setApiKeyFile(String apiKeyFile) {
        this.apiKeyFile = apiKeyFile;
    }

    private static void deleteCalendar(String calendarTitle) {
        try {
            Calendar googleCalendar = createAndGetCalendar(calendarTitle);
            CalendarListEntry googleCalendard = client.calendarList().get(googleCalendar.getId()).execute();

            if (client.acl().list(googleCalendar.getId()).execute().getItems().get(0).getRole().equals("reader") ||
                    client.calendarList().get(googleCalendar.getId()).execute().isPrimary()) {
                logger.warn("Calendar " + calendarTitle + " cannot be removed - skipping");
            } else {
                logger.warn("Deleting obsolete calendar: " + calendarTitle + ": " + googleCalendard.getId());
                client.calendars().delete(googleCalendard.getId()).execute();
            }

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                logger.warn("Calendar: " + calendarTitle + " to be deleted but not found");
            }
        } catch (IOException e) {
            logger.error("Problem removing: " + calendarTitle);
            e.printStackTrace();
        }
    }

    private static Set<String> getGoogleCalendars() throws IOException {
        return client.calendarList().list().execute().getItems().stream().map(CalendarListEntry::getSummary).collect(Collectors.toSet());
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    private static void setLoggingToLevel(String level) {
        if (null != level) {
            ch.qos.logback.classic.Logger root = getRootLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.toLevel(level, Level.DEBUG));
        }

        // we suppress calendar TRACE level, because not needed for me
        Stream.of("net.fortuna.ical4j.data.CalendarParserImpl",
                "net.fortuna.ical4j.data.FoldingWriter",
                "net.fortuna.ical4j.util.Configurator",
                "net.fortuna.ical4j.model.TimeZoneRegistryImpl")
                .forEach(suppress -> getRootLogger(suppress).setLevel(Level.ERROR));
    }

    private static ch.qos.logback.classic.Logger getRootLogger(String rootLoggerName) {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                rootLoggerName
        );
    }

}


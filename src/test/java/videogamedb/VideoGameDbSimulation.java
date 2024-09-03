package videogamedb;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class VideoGameDbSimulation extends Simulation {

    /*
    HTTP Configuration

    Base URL is 'videogamedb.uk/api'
     */
    private HttpProtocolBuilder httpProtocol =
            http
                    .baseUrl("https://videogamedb.uk/api")
                    .acceptHeader("application/json")
                    .contentTypeHeader("application/json");

    // Runtime Parameters
    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
    private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "10"));

    // Feeder for test data using the random strategy
    private static FeederBuilder.FileBased<Object> jsonFeeder = jsonFile("data/gameJsonFileFeeder.json").random();

    // Before block allows us to see what the env variables are set to
    @Override
    public void before() {
        System.out.printf("Running test with %d users%n", USER_COUNT);
        System.out.printf("Running users over %d seconds%n", RAMP_DURATION);
    }

    // HTTP Calls
    private static ChainBuilder getAllGames =
            exec(http("Get all games")
                    .get("/videogame"));

    // Need to extract out the token and save into the script using check
    // jmesPath extracts out the value of token and saves it in our script as jwtToken for re-use
    private static ChainBuilder authenticate =
            exec(http("Authenticate")
                    .post("/authenticate")
                    .body(StringBody("{\n" +
                            "  \"password\": \"admin\",\n" +
                            "  \"username\": \"admin\"\n" +
                            "}"))
                    .check(jmesPath("token").saveAs("jwtToken")));

    /*
    Here we are reusing the token to authenticate our endpoint.
    Use a JSON template for the POST body
     */
    private static ChainBuilder createNewGame =
            feed(jsonFeeder)
                    .exec(http("Create New Game - #{name}") // Replaced by whatever name has been selected
                            .post("/videogame")
                            .header("Authorization", "Bearer #{jwtToken}")
                            .body(ElFileBody("bodies/newGameTemplate.json")).asJson());

    /*
    Get last posted game
     */
    private static ChainBuilder getLastPostedGame =
            exec(http("Get Last Posted Game - #{name}")
                    .get("/videogame/#{id}")
                    .check(jmesPath("name").isEL("#{name}"))); // Check if name parameter matches JSON

    /*
    Delete the last posted game and we need to authenticate
     */
    private static ChainBuilder deleteLastPostedGame =
            exec(http("Delete Game - #{name}")
                    .delete("/videogame/#{id}")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .check(bodyString().is("Video game deleted")));


    /*
    Scenario Definition

    'Video Game DB Stress Test' is the scenario name
    'Get all games' is what appears in the report
    Makes a GET request to <baseURL>/videogame

     */
    // 1. Get all video games
    // 2. Authenticate with the application
    // 3. Create a new game
    // 4. Get details of newly created game
    // 5. Delete newly created game
    private ScenarioBuilder scn =
            scenario("Video Game DB Stress Test")
                    .exec(getAllGames)
                    .pause(2) // Pause for 2 seconds
                    .exec(authenticate)
                    .pause(2)
                    .exec(createNewGame)
                    .pause(2)
                    .exec(getLastPostedGame)
                    .pause(2)
                    .exec(deleteLastPostedGame);
    /*
    USER_COUNT and RAMP_DURATION are provided by passing parameters into maven
    Default values specified in the field
     */
    {
        setUp(
                scn.injectOpen(
                        nothingFor(5), // Does nothing for the first 5 seconds
                        rampUsers(USER_COUNT).during(RAMP_DURATION) // By default insert 5 users evenly over 10 seconds
                )
        ).protocols(httpProtocol);
    }
}

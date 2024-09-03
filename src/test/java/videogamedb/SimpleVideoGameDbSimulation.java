package videogamedb;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class SimpleVideoGameDbSimulation extends Simulation {
    //Use the HTTP protocol and the base URL to send requests to
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api")
            .acceptHeader("application/json");

    /*
    Scenario Definition

    'Video Game DB Stress Test' is the scenario name
    'Get all games' is what appears in the report

    Makes a single GET request to the /videogame endpoint
     */
    private ScenarioBuilder scn = scenario("Video Game DB Stress Test")
            .exec(http("Get all games").get("/videogame"));

    // Load Simulation using a static initializer with 1 user and tie it to our protocol
    {
        setUp(scn.injectOpen(OpenInjectionStep.atOnceUsers(1)).protocols(httpProtocol));
    }
}

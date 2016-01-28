package test;

import java.util.*;
import java.lang.*;
import java.text.*;

import javax.ws.rs.core.Response;

/**
 * Created by cuonght on 1/17/2016.
 */
public class RestTest {
    public static void main(String[] args) {
        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date (month/day/year)
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        // Get the date today using Calendar object.
        Date today = Calendar.getInstance().getTime();
        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.
        String reportDate = df.format(today);

        // Print what date is today!
        System.out.println("Report Date: " + reportDate);

        Map<String, Object> jSendResponse = new HashMap<String, Object>();
        jSendResponse.put("wind_speed", 1);
        jSendResponse.put("cuonght", "beo");
        System.out.println("===hihi===");
        Response xxx = Response.ok().header("Access-Control-Allow-Origin","*").entity(jSendResponse).build();
        System.out.println(xxx);
    }
}

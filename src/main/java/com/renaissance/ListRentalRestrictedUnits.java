package com.renaissance;

import org.json.JSONObject;

import com.github.jillow.core.ZillowApiService;
import com.github.jillow.util.ApplicationProperties;
import com.pholser.util.properties.PropertyBinder;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ListRentalRestrictedUnits {

  private ZillowApiService service = new ZillowApiService();
  private ApplicationProperties properties;

  public void init() throws Exception {
    PropertyBinder<ApplicationProperties> binder = PropertyBinder.forType(ApplicationProperties.class);
    properties = binder.bind(ClassLoader.class.getResourceAsStream("/application.properties"));
  }

  public Map<String, Date> find(String propertyListFile) throws Exception {
    Map<String, Date> list = new HashMap<>();
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
    try (BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.class.getResourceAsStream(propertyListFile)))) {
      String streetAddress;
      while ((streetAddress = br.readLine()) != null && streetAddress != null && !streetAddress.isEmpty()) {
        int retries = 2;
        while (retries > 0) {
          try {
            JSONObject resp = service.getDeepSearchResultsJson(properties.zwsId(), streetAddress, "San Jose, CA 95134", false);

            JSONObject details = resp
                .getJSONObject("SearchResults:searchresults")
                .getJSONObject("response")
                .getJSONObject("results")
                .getJSONObject("result");

            String lastSoldDateStr = details.getString("lastSoldDate");
//        int lastSoldPrice = details
//            .getJSONObject("lastSoldPrice")
//            .getInt("content");
            Date lastSoldDate = dateFormat.parse(lastSoldDateStr);

            list.put(streetAddress, lastSoldDate);
            break;
          } catch (Exception e) {
            retries--;
            System.out.printf("Failed to get response for %s\n", streetAddress);
          }
        }
      }
    }

    return list;
  }

  public static void main(String[] args) throws Exception {
    ListRentalRestrictedUnits obj = new ListRentalRestrictedUnits();
    obj.init();
    Map<String, Date> list = obj.find("/sjr-property-units");
    try (PrintWriter writer = new PrintWriter(new FileOutputStream("/tmp/rental-units"))) {
      Date cutOffDate = new Date(2013 - 1900, 12, 31);
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      for(Entry<String, Date> entry : list.entrySet()) {
        if (entry.getValue().after(cutOffDate)) {
          writer.printf("%s,%s\n", entry.getKey(), dateFormat.format(entry.getValue()));
        }
      }
    }
  }
}

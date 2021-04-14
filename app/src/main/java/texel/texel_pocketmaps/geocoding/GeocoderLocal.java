package texel.texel_pocketmaps.geocoding;

import android.content.Context;
import android.content.res.Resources;
import android.location.Address;
import android.util.Log;

import com.graphhopper.routing.util.AllEdgesIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.map.MapHandler;
import texel.texel_pocketmaps.model.listeners.OnProgressListener;
import texel.texel_pocketmaps.util.GeoMath;
import texel.texel_pocketmaps.util.Variable;

public class GeocoderLocal {
    public final static int ADDR_TYPE_FOUND = 0;
    public final static int ADDR_TYPE_CITY = 1;
    public final static int ADDR_TYPE_CITY_EN = 2;
    public final static int ADDR_TYPE_POSTCODE = 3;
    public final static int ADDR_TYPE_STREET = 4;
    public final static int ADDR_TYPE_COUNTRY = 5;
    public final static int BIT_MULT = 1;
    public final static int BIT_EXPL = 2;
    public final static int BIT_CITY = 4;
    public final static int BIT_STREET = 8;

    private final Locale locale;
    private boolean bMultiMatchOnly;
    private boolean bExplicitSearch;
    private boolean bStreetNodes;
    private boolean bCityNodes;

    public GeocoderLocal(Context context, Locale locale) {
        this.locale = locale;
    }

    public static boolean isPresent() {
        return true;
    }

    private boolean isContainExactWord(String source, String subItem) {
        String pattern = "\\b" + subItem + "\\b";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(source);
        return m.find();
    }

    private String convertAzerbaijanAlphabet(String line) {
        return line
                .replaceAll("ı", "i")
                .replaceAll("ə", "e")
                .replaceAll("ş", "s")
                .replaceAll("sh", "ş")
                .replaceAll("w", "ş")
                .replaceAll("ç", "c")
                .replaceAll("ğ", "g")
                .replaceAll("ü", "u")
                .replaceAll("ö", "o");
    }

    private Address createAddressForList(String name, double latitude, double longitude) {
        Address address = new Address(locale);
        address.setAddressLine(GeocoderLocal.ADDR_TYPE_COUNTRY, Variable.getVariable().getCountry());
        address.setLatitude(latitude);
        address.setLongitude(longitude);
        address.setAddressLine(GeocoderLocal.ADDR_TYPE_STREET, name);
        return address;
    }

    public void readRawTextFile(ArrayList<Address> addressList, String search_text) {
        try {
            String line;
            String[] splitSearchText = search_text.split(" ");
            Resources resources = Variable.getVariable().getContext().getResources();

            search_text = convertAzerbaijanAlphabet(search_text).trim();
            ArrayList<String> matchingLineList = new ArrayList<>();

            InputStream inputStream = resources.openRawResource(R.raw.mylocations);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            addAllWordsContainedLines(bufferedReader, matchingLineList, search_text);

            inputStream = resources.openRawResource(R.raw.mylocations);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            addExactElements(bufferedReader, matchingLineList, search_text);

            inputStream = resources.openRawResource(R.raw.mylocations);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            for (String word : splitSearchText)
                addExactElements(bufferedReader, matchingLineList, word.trim());

            inputStream = resources.openRawResource(R.raw.mylocations);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = bufferedReader.readLine()) != null) {
                line = convertAzerbaijanAlphabet(line);
                for (String word : splitSearchText)
                    if (line.contains(word.trim()) && !matchingLineList.contains(line)) {
                        matchingLineList.add(line);
                        break;
                    }
            }
            for (String txt : matchingLineList) addAddressToList(addressList, txt);
        } catch (Exception e) {
            Log.d("MyError", e.toString());
        }
    }

    private void addAllWordsContainedLines(BufferedReader bufferedReader, ArrayList<String> matchingLineList, String search_text) {
        String line;
        boolean b;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                line = convertAzerbaijanAlphabet(line);
                b = true;
                for (String word : search_text.split(" "))
                    if (!line.contains(word.trim())) {
                        b = false;
                        break;
                    }
                if (b) matchingLineList.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addAddressToList(ArrayList<Address> addressList, String line) {
        String name = line.substring(0, line.indexOf("*"));
        String location = line.substring(line.indexOf("*") + 1);
        addressList.add(createAddressForList(
                name,
                Double.parseDouble(location.substring(location.indexOf(" ") + 1)),
                Double.parseDouble(location.substring(0, location.indexOf(" ")))));
    }

    private void addExactElements(BufferedReader bufferedReader, ArrayList<String> matchingLineList, String search_text) {
        search_text = search_text.trim();
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                line = convertAzerbaijanAlphabet(line);
                if (isContainExactWord(line, search_text) && !matchingLineList.contains(line))
                    matchingLineList.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Address> getFromLocationName(String searchS, int maxCount, OnProgressListener progressListener) throws IOException {
        getSettings();
        progressListener.onProgress(2);
        ArrayList<Address> addrList = new ArrayList<Address>();
        if (bCityNodes && !bMultiMatchOnly) {
            ArrayList<Address> nodes = findCity(searchS, maxCount);
            if (nodes == null) return null;
            addrList.addAll(nodes);
        }
        progressListener.onProgress(5);
        if (addrList.size() < maxCount && bStreetNodes) {
            ArrayList<Address> nodes = searchNodes(searchS, maxCount - addrList.size(), progressListener);
            if (nodes != null) addrList.addAll(nodes);
        }

        if (addrList.size() == 0) {
            List<Address> list;
            GeocoderGlobal geocoderGlobal = new GeocoderGlobal(locale);
            list = geocoderGlobal.find_osm(Variable.getVariable().getContext(), searchS);
            if (list != null)
                addrList.addAll(list);

            if (addrList.size() == 0) {
                list = geocoderGlobal.find_google(Variable.getVariable().getContext(), searchS);
                if (list != null)
                    addrList.addAll(list);
            }

            if (addrList.size() == 0) return null;
        }
        return addrList;
    }

    private void getSettings() {
        int bits = Variable.getVariable().getOfflineSearchBits();
        bMultiMatchOnly = (bits & BIT_MULT) > 0;
        bExplicitSearch = (bits & BIT_EXPL) > 0;
        bCityNodes = (bits & BIT_CITY) > 0;
        bStreetNodes = (bits & BIT_STREET) > 0;
    }

    /**
     * For more information of street-matches.
     **/
    private String findNearestCity(double lat, double lon) {
        String mapsPath = Variable.getVariable().getMapsFolder().getAbsolutePath();
        mapsPath = new File(mapsPath, Variable.getVariable().getCountry() + "-gh").getPath();
        mapsPath = new File(mapsPath, "city_nodes.txt").getPath();
        String nearestName = null;
        double nearestDist = 0;
        String curName = "";
        double curLat = 0;
        double curLon = 0;
        double curDist = 0;
        try (FileReader r = new FileReader(mapsPath);
             BufferedReader br = new BufferedReader(r)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (GeocoderGlobal.isStopRunningActions()) {
                    return null;
                }
                if (line.startsWith("name=")) {
                    curName = readString("name", line);
                } else if (line.startsWith("name:en=")) {
                    if (curName.isEmpty()) {
                        curName = readString("name:en", line);
                    }
                } else if (line.startsWith("lat=")) {
                    curLat = readDouble("lat", line);
                } else if (line.startsWith("lon=")) {
                    if (curName.isEmpty()) {
                        continue;
                    }
                    curLon = readDouble("lon", line);
                    curDist = GeoMath.fastDistance(curLat, curLon, lat, lon);
                    if (nearestName == null || curDist < nearestDist) {
                        nearestDist = curDist;
                        nearestName = curName;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nearestName;
    }

    private ArrayList<Address> findCity(String searchS, int maxCount) throws IOException {
        ArrayList<Address> result = new ArrayList<Address>();
        CityMatcher cityMatcher = new CityMatcher(searchS, bExplicitSearch);

        String mapsPath = Variable.getVariable().getMapsFolder().getAbsolutePath();
        mapsPath = new File(mapsPath, Variable.getVariable().getCountry() + "-gh").getPath();
        mapsPath = new File(mapsPath, "city_nodes.txt").getPath();
        Address curAddress = new Address(locale);
        try (FileReader r = new FileReader(mapsPath);
             BufferedReader br = new BufferedReader(r)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (GeocoderGlobal.isStopRunningActions()) {
                    return null;
                }
                if (result.size() >= maxCount) {
                    break;
                }
                if (line.startsWith("name=")) {
                    curAddress = clearAddress(curAddress);
                    String name = readString("name", line);
                    curAddress.setAddressLine(ADDR_TYPE_CITY, name);
                    boolean isMatching = cityMatcher.isMatching(name, false);
                    if (isMatching) {
                        result.add(curAddress);
                        curAddress.setAddressLine(ADDR_TYPE_COUNTRY, Variable.getVariable().getCountry());
                        curAddress.setAddressLine(ADDR_TYPE_FOUND, name);
                        log("Added address: " + name);
                    }
                } else if (line.startsWith("name:en=")) {
                    String name = readString("name:en", line);
                    curAddress.setAddressLine(ADDR_TYPE_CITY_EN, name);
                    if (curAddress.getAddressLine(ADDR_TYPE_COUNTRY) == null) { // Is still not attached!
                        boolean isMatching = cityMatcher.isMatching(name, false);
                        if (isMatching) {
                            result.add(curAddress);
                            curAddress.setAddressLine(ADDR_TYPE_COUNTRY, Variable.getVariable().getCountry());
                            curAddress.setAddressLine(ADDR_TYPE_FOUND, name);
                            log("Added address: " + name);
                        }
                    }
                } else if (line.startsWith("post=")) {
                    String name = readString("post", line);
                    curAddress.setAddressLine(ADDR_TYPE_POSTCODE, name);
                    if (curAddress.getAddressLine(ADDR_TYPE_COUNTRY) == null) { // Is still not attached!
                        boolean isMatching = cityMatcher.isMatching(name, true);
                        if (isMatching) {
                            result.add(curAddress);
                            curAddress.setAddressLine(ADDR_TYPE_COUNTRY, Variable.getVariable().getCountry());
                            curAddress.setAddressLine(ADDR_TYPE_FOUND, name);
                            log("Added address: " + name);
                        }
                    }
                } else if (line.startsWith("lat=")) {
                    curAddress.setLatitude(readDouble("lat", line));
                } else if (line.startsWith("lon=")) {
                    curAddress.setLongitude(readDouble("lon", line));
                }
            }
        } catch (IOException e) {
            throw e;
        }
        return result;
    }

    private Address clearAddress(Address curAddress) {
        if (curAddress.getAddressLine(ADDR_TYPE_COUNTRY) == null) { // Clear this curAddress for reuse!
            curAddress.clearLatitude();
            curAddress.clearLongitude();
            for (int i = 10; i > 0; i--) {
                curAddress.setAddressLine(i, null);
            }
            curAddress.setAddressLine(ADDR_TYPE_COUNTRY, null);
        } else {
            curAddress = new Address(locale);
        }
        return curAddress;
    }

    private double readDouble(String key, String txt) {
        String s = readString(key, txt);
        if (s == null) {
            Log.e(GeocoderLocal.class.getName(), "Double for key not found: " + key);
            return 0;
        }
        return Double.parseDouble(s);
    }

    private String readString(String key, String txt) {
        return txt.substring(key.length() + 1);
    }

    /**
     * Search all edges for matching text.
     **/
    ArrayList<Address> searchNodes(String searchingText, int maxMatches, OnProgressListener progressListener) {
        searchingText = searchingText.toLowerCase();
        ArrayList<Address> addressList = new ArrayList<>();
        StreetMatcher streetMatcher = new StreetMatcher(searchingText, bExplicitSearch);

        readRawTextFile(addressList, searchingText);

        AllEdgesIterator edgeList = MapHandler.getMapHandler().getAllEdges();

        log("SEARCH_EDGE Start ...");
        int counter = 0;
        int lastProgress = 5;
        while (edgeList.next()) {
            counter++;
            if (GeocoderGlobal.isStopRunningActions()) return null;
            if (edgeList.getName().isEmpty()) continue;
            if (edgeList.fetchWayGeometry(0).isEmpty()) continue;

            int newProgress = (counter * 100) / edgeList.length();
            if (newProgress > lastProgress) {
                progressListener.onProgress((counter * 100) / edgeList.length());
                lastProgress = newProgress;
            }
            if (streetMatcher.isMatching(edgeList.getName(), false)) {
                log("SEARCH_EDGE Status: " + counter + "/" + edgeList.length());
                boolean b = StreetMatcher.addToList(addressList,
                        edgeList.getName(),
                        edgeList.fetchWayGeometry(0).get(0).lat,
                        edgeList.fetchWayGeometry(0).get(0).lon,
                        locale);
                if (b) {
                    String c = findNearestCity(edgeList.fetchWayGeometry(0).get(0).lat, edgeList.fetchWayGeometry(0).get(0).lon);
                    if (bMultiMatchOnly && !streetMatcher.isMatching(c, false)) {
                        addressList.remove(addressList.size() - 1);
                    } else {
                        log("SEARCH_EDGE found=" + edgeList.getName() + " on " + c);
                        addressList.get(addressList.size() - 1).setAddressLine(ADDR_TYPE_CITY, c);
                    }
                }
            }
            if (addressList.size() >= maxMatches) {
                break;
            }
        }
        log("SEARCH_EDGE Stop on length=" + addressList.size());
        return addressList;
    }

    private void log(String str) {
        Log.i(GeocoderLocal.class.getName(), str);
    }
}

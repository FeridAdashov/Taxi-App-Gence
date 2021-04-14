package texel.texel_pocketmaps.navigator;

import com.graphhopper.util.Instruction;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.map.Navigator;
import texel.texel_pocketmaps.util.UnitCalculator;

public class NaviInstruction {
    String curStreet;
    String nextInstruction;
    String nextInstructionShort;
    String nextInstructionShortFallback;
    long fullTime;
    String fullTimeString;
    double nextDistance;
    int nextSign;
    int nextSignResource;

    public NaviInstruction(Instruction in, Instruction nextIn, long fullTime) {
        if (nextIn != null) {
            nextSign = nextIn.getSign();
            nextSignResource = Navigator.getNavigator().getDirectionSignHuge(nextIn);
            nextInstruction = Navigator.getNavigator().getDirectionDescription(nextIn, true);
            nextInstructionShort = Navigator.getNavigator().getDirectionDescription(nextIn, false);
            nextInstructionShortFallback = Navigator.getNavigator().getDirectionDescriptionFallback(nextIn, false);
        } else {
            nextSign = in.getSign(); // Finished?
            nextSignResource = Navigator.getNavigator().getDirectionSignHuge(in);
            nextInstruction = Navigator.getNavigator().getDirectionDescription(in, true);
            nextInstructionShort = Navigator.getNavigator().getDirectionDescription(in, false);
            nextInstructionShortFallback = Navigator.getNavigator().getDirectionDescriptionFallback(in, false);
        }
        if (nextSignResource == 0) {
            nextSignResource = R.drawable.ic_2x_continue_on_street;
        }
        nextDistance = in.getDistance();
        this.fullTime = fullTime;
        fullTimeString = Navigator.getNavigator().getTimeString(fullTime);
        curStreet = in.getName();
        if (curStreet == null) {
            curStreet = "";
        }
    }

    public double getNextDistance() {
        return nextDistance;
    }

    public String getFullTimeString() {
        return fullTimeString;
    }

    public int getNextSignResource() {
        return nextSignResource;
    }

    public String getCurStreet() {
        return curStreet;
    }

    public String getNextInstruction() {
        return nextInstruction;
    }

    public String getNextDistanceString() {
        if (nextDistance > (UnitCalculator.METERS_OF_KM * 10.0)) {
            String unit = " " + UnitCalculator.getUnit(true);
            return UnitCalculator.getBigDistance(nextDistance, 0) + unit;
        }
        String unit = " " + UnitCalculator.getUnit(false);
        return UnitCalculator.getShortDistance(nextDistance) + unit;
    }


    public void updateDist(double partDistance) {
        nextDistance = partDistance;
    }
}

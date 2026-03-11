package com.liatrio.dora.insights;

import com.liatrio.dora.dto.WeekDataPoint;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrendDirectionCalculator {

    public TrendDirection calculate(List<WeekDataPoint> timeSeries) {
        if (timeSeries == null || timeSeries.size() < 2) {
            return TrendDirection.STABLE;
        }

        int size = timeSeries.size();
        List<WeekDataPoint> firstHalf = timeSeries.subList(0, size / 2);
        List<WeekDataPoint> secondHalf = timeSeries.subList(size / 2, size);

        double firstAvg = average(firstHalf);
        double secondAvg = average(secondHalf);

        if (firstAvg == 0.0) {
            return TrendDirection.STABLE;
        }

        double delta = (secondAvg - firstAvg) / firstAvg;

        if (delta > 0.10) {
            return TrendDirection.IMPROVING;
        } else if (delta < -0.10) {
            return TrendDirection.DECLINING;
        } else {
            return TrendDirection.STABLE;
        }
    }

    private double average(List<WeekDataPoint> points) {
        return points.stream()
                .mapToDouble(WeekDataPoint::value)
                .average()
                .orElse(0.0);
    }
}

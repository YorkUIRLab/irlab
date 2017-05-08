package org.utils;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Created by sonic on 11/03/17.
 */
public class StatUtils {

    /**
     * Normalize (standardize) the sample, so it is has a mean of 0 and a standard deviation of 1.
     *
     * @param sample Sample to normalize.
     * @return normalized (standardized) sample.
     * @since 2.2
     */
    public static double[] normalize(final double[] sample) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        // Add the data from the series to stats
        for (double aSample : sample) stats.addValue(aSample);

        // Compute mean and standard deviation
        double mean = stats.getMean();
        double standardDeviation = stats.getStandardDeviation();

        // initialize the standardizedSample, which has the same length as the sample
        double[] standardizedSample = new double[sample.length];

        for (int i = 0; i < sample.length; i++) {
            // z = (x- mean)/standardDeviation
            standardizedSample[i] = (sample[i] - mean) / standardDeviation;
        }
        return standardizedSample;
    }

}

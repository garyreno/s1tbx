/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.gpf.geometric;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.TiePointGeoCoding;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit test for SRGROperator.
 */
public class TestSRGROperator {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new SRGROp.Spi();

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX"};
    private String[] exceptionExemptions = {"conversion has already been applied", "not supported", "GeoCoding is null",
            "not be map projected", "Source product should first be deburst"};

    /**
     * Tests SRGR operator with a 4x16 "DETECTED" test product.
     */
    @Test
    public void testSRGROperator() throws Exception {

        Product sourceProduct = createTestProduct(16, 4);

        SRGROp op = (SRGROp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setNumOfRangePoints(6);
        op.setSourceBandName("band1");

        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[28];
        band.readPixels(0, 0, 7, 4, floatValues, ProgressMonitor.NULL);

        double[] warpPolynomialCoef = op.getWarpPolynomialCoef();
        double[] expectedWarpCoeff = {-0.267942583741615, 1.039902702338278, -0.001031550514190, 0.000014679621756,
                -0.000000075665832};
        for (int i = 0; i < warpPolynomialCoef.length; i++) {
            assertTrue(Math.abs(expectedWarpCoeff[i] - warpPolynomialCoef[i]) < 0.000001);
        }
        // compare with expected outputs:
        float[] expectedValues = {1.0f, 3.0221484f, 5.0534487f, 7.0752897f, 9.097291f, 11.119023f, 13.130011f, 17.0f,
                19.022148f, 21.053448f, 23.07529f, 25.09729f, 27.119024f, 29.13001f, 33.0f, 35.02215f, 37.053448f, 39.07529f,
                41.09729f, 43.119022f, 45.130013f, 49.0f, 51.02215f, 53.053448f, 55.07529f, 57.09729f, 59.119022f, 61.130013f};
        //assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final MetadataAttribute attr = abs.getAttribute(AbstractMetadata.srgr_flag);
        assertTrue(attr.getData().getElemBoolean());
    }


    /**
     * Creates a 4-by-16 test product as shown below:
     * 1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16
     * 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32
     * 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48
     * 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63 64
     */
    private static Product createTestProduct(int w, int h) {

        final Product testProduct = new Product("source", "ASA_APS_1P", w, h);

        // create a Band: band1
        final Band band1 = testProduct.addBand("band1", ProductData.TYPE_INT32);
        int[] intValues = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1;
        }
        band1.setData(ProductData.createInstance(intValues));

        // create abstracted metadata
        final MetadataElement abs = new MetadataElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);

        abs.addAttribute(new MetadataAttribute(AbstractMetadata.MISSION,
                ProductData.createInstance("ENVISAT"), false));

        abs.addAttribute(new MetadataAttribute(AbstractMetadata.PASS,
                ProductData.createInstance("DESCENDING"), false));

        abs.addAttribute(new MetadataAttribute(AbstractMetadata.srgr_flag,
                ProductData.createInstance(new byte[]{0}), false));

        abs.addAttribute(new MetadataAttribute(AbstractMetadata.range_spacing,
                ProductData.createInstance(new float[]{7.0F}), false));

        abs.addAttribute(new MetadataAttribute(AbstractMetadata.azimuth_spacing,
                ProductData.createInstance(new float[]{4.0F}), false));

        abs.addAttribute(new MetadataAttribute(AbstractMetadata.slant_range_to_first_pixel,
                ProductData.createInstance(new float[]{800000.0F}), false));

        // create incidence angle tie point grid
        float[] incidence_angle = new float[w * h];
        Arrays.fill(incidence_angle, 30.0f);
        testProduct.addTiePointGrid(new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, w, h, 0, 0, 1, 1, incidence_angle));

        // create lat/lon tie point grids
        float[] lat = new float[w * h];
        float[] lon = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                lon[i] = 13.20f + x / 10000.0f;
                lat[i] = 51.60f + y / 10000.0f;
            }
        }
        TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, w, h, 0, 0, 1, 1, lat);
        TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, w, h, 0, 0, 1, 1, lon);
        testProduct.addTiePointGrid(latGrid);
        testProduct.addTiePointGrid(lonGrid);

        AbstractMetadata.getOriginalProductMetadata(testProduct).addElement(abs);

        // create geoCoding
        testProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        return testProduct;
    }

    @Test
    public void testProcessAllASAR() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsASAR, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllERS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsERS, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllALOS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsALOS, "ALOS PALSAR CEOS", null, exceptionExemptions);
    }

    @Test
    public void testProcessAllRadarsat2() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsRadarsat2, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllTerraSARX() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsTerraSarX, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllCosmo() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsCosmoSkymed, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllSentinel1() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsSentinel1, null, exceptionExemptions);
    }
}

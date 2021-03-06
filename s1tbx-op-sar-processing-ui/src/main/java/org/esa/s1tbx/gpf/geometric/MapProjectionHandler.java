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

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.crs.CrsSelectionPanel;
import org.esa.snap.framework.ui.crs.CustomCrsForm;
import org.esa.snap.framework.ui.crs.PredefinedCrsForm;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.ProductUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JPanel;
import java.awt.Insets;

/**
 * Helper for adding map projection components into an operator UI
 */
public class MapProjectionHandler {

    private final CrsSelectionPanel crsSelectionPanel;
    private CoordinateReferenceSystem crs;

    public MapProjectionHandler() {
        crsSelectionPanel = createCRSPanel();
    }

    private static CrsSelectionPanel createCRSPanel() {
        final SnapApp.SnapContext appContext = new SnapApp.SnapContext();
        final CustomCrsForm customCrsForm = new CustomCrsForm(appContext);
        final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(appContext);

        return new CrsSelectionPanel(customCrsForm, predefinedCrsForm);
    }

    public void initParameters(final String mapProjection, final Product[] sourceProducts) {
        crs = getCRS(mapProjection, sourceProducts);
    }

    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    public String getCRSName() {
        if (crs != null) {
            return crs.getName().getCode();
        }
        return "";
    }

    private CoordinateReferenceSystem getCRS(final String mapProjection, final Product[] sourceProducts) {
        final CoordinateReferenceSystem theCRS = parseCRS(mapProjection);
        if (theCRS == null)
            return getCRSFromDialog(sourceProducts);
        return theCRS;
    }

    static CoordinateReferenceSystem parseCRS(final String mapProjection) {
        try {
            if (mapProjection != null && !mapProjection.isEmpty())
                return CRS.parseWKT(mapProjection);
        } catch (Exception e) {
            try {
                return CRS.decode(mapProjection, true);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        return null;
    }

    private CoordinateReferenceSystem getCRSFromDialog(final Product[] sourceProducts) {
        try {
            if (sourceProducts == null || sourceProducts[0] == null)
                return crsSelectionPanel.getCrs(new GeoPos(0, 0));
            return crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(sourceProducts[0]));
        } catch (Exception e) {
            SnapDialogs.showError("Unable to create coodinate reference system");
        }
        return null;
    }

    public void promptForFeatureCrs(final Product[] sourceProducts) {

        final ModalDialog dialog = new ModalDialog(null,
                "Map Projection",
                ModalDialog.ID_OK_CANCEL_HELP, "mapProjection");

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellPadding(0, 0, new Insets(4, 10, 4, 4));
        final JPanel contentPanel = new JPanel(tableLayout);
        contentPanel.add(crsSelectionPanel);
        dialog.setContent(contentPanel);
        if (dialog.show() == ModalDialog.ID_OK) {
            crs = getCRSFromDialog(sourceProducts);
        }
    }
}

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
package org.esa.s1tbx.dataio.ceos.ers;

import org.esa.s1tbx.dataio.ceos.CEOSProductReaderPlugIn;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;

import java.io.File;

/**
 * The ReaderPlugIn for ERS CEOS products.
 */
public class ERSProductReaderPlugIn extends CEOSProductReaderPlugIn {

    public ERSProductReaderPlugIn() {
        constants = new ERSConstants();
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new ERSProductReader(this);
    }

    @Override
    protected DecodeQualification checkProductQualification(final File file) {
        final String name = file.getName().toUpperCase();
        if (name.startsWith("VDF")) {
            final ERSProductReader reader = new ERSProductReader(this);
            return reader.checkProductQualification(file);
        }
        return DecodeQualification.UNABLE;
    }

}

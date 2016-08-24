/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.kompsat5;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.io.SARReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.File;
import java.io.IOException;

/**
 * The product reader for CosmoSkymed products.
 */
public class Kompsat5Reader extends SARReader {

    private final ProductReaderPlugIn readerPlugIn;

    private enum Formats {HDF, GEOTIFF}

    private Formats format;
    private K5Format k5Reader;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public Kompsat5Reader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.readerPlugIn = readerPlugIn;
    }

    private static Formats determineFormat(final File inputFile) {
        final String name = inputFile.getName().toLowerCase();
        if (name.endsWith(".h5"))
            return Formats.HDF;
        return Formats.GEOTIFF;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        try {
            final File inputFile = ReaderUtils.getFileFromInput(getInput());
            format = determineFormat(inputFile);

            if (format.equals(Formats.HDF)) {
                k5Reader = new K5HDF(readerPlugIn, this);
            } else {
                k5Reader = new K5GeoTiff(readerPlugIn, this);
            }

            Product product = k5Reader.open(inputFile);
            setQuicklookBandName(product);

            product.getGcpGroup();
            product.setModified(false);

            return product;
        } catch (Exception e) {
            handleReaderException(e);
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        k5Reader.close();
        super.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        k5Reader.readBandRasterDataImpl(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                                        sourceStepX, sourceStepY, destBand, destOffsetX,
                                        destOffsetY, destWidth, destHeight, destBuffer, pm);
    }
}
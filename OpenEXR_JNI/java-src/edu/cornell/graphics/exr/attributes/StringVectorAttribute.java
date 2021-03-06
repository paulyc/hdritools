/*============================================================================
  HDRITools - High Dynamic Range Image Tools
  Copyright 2008-2012 Program of Computer Graphics, Cornell University

  Distributed under the OSI-approved MIT License (the "License");
  see accompanying file LICENSE for details.

  This software is distributed WITHOUT ANY WARRANTY; without even the
  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the License for more information.
 -----------------------------------------------------------------------------
 Primary author:
     Edgar Velazquez-Armendariz <cs#cornell#edu - eva5>
============================================================================*/

package edu.cornell.graphics.exr.attributes;

import edu.cornell.graphics.exr.EXRIOException;
import edu.cornell.graphics.exr.io.XdrInput;
import edu.cornell.graphics.exr.io.XdrOutput;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code TypedAttribute} subclass holding a {@code List<String>} value.
 * 
 * <p>This attribute is named after the one in the original C++ OpenEXR library
 * thus "vector" refers to its original implementation using
 * {@code std::vector<std::string>}, it does <em>not</em> have anything to do
 * with {@code java.util.Vector<E>}.</p>
 * 
 * @since OpenEXR-JNI 2.1
 */
public final class StringVectorAttribute extends TypedAttribute<List<String>> {
    
    public StringVectorAttribute() {
        // empty
    }
    
    public StringVectorAttribute(List<String> value) {
        super(value);
    }

    @Override
    public String typeName() {
        return "stringvector";
    }

    @Override
    public void readValueFrom(XdrInput input, int size, int version)
            throws EXRIOException {
        int read = 0;
        ArrayList<String> lst = new ArrayList<>();
        while (read < size) {
            int length = input.readInt();
            String s = input.readUTF8(length);
            lst.add(s);
            read += length + 4;
        }
        checkSize(read, size);
        setValue(lst);
    }

    @Override
    protected void writeValueTo(XdrOutput output) throws EXRIOException {
        final Charset UTF8 = Charset.forName("UTF-8");
        final List<String> lst = getValue();
        for (String str : lst) {
            final byte[] b = str.getBytes(UTF8);
            output.writeInt(b.length);
            output.writeByteArray(b);
        }
    }

    @Override
    protected List<String> cloneValue() {
        return new ArrayList<>(value);
    }
    
}

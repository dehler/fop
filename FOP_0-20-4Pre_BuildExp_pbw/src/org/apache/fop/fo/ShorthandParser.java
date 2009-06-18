/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo;

public interface ShorthandParser {
    public Property getValueForProperty(String propName,
                                        Property.Maker maker,
                                        PropertyList propertyList);
}
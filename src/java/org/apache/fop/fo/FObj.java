/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.fop.fo.flow.Marker;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.PropertyMaker;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/**
 * Base class for representation of formatting objects and their processing.
 */
public class FObj extends FONode implements Constants {
    public static PropertyMaker[] propertyListTable = null;
    
    /** Formatting properties for this fo element. */
    protected PropertyList propertyList;

    /** Property manager for providing refined properties/traits. */
    protected PropertyManager propMgr;

    /** The immediate child nodes of this node. */
    public ArrayList childNodes = null;

    /** Used to indicate if this FO is either an Out Of Line FO (see rec)
        or a descendant of one.  Used during validateChildNode() FO 
        validation.
    */
    private boolean isOutOfLineFODescendant = false;

    /** Markers added to this element. */
    protected Map markers = null;

    /** Dynamic layout dimension. Used to resolve relative lengths. */
    protected Map layoutDimension = null;

    /**
     * Create a new formatting object.
     * All formatting object classes extend this class.
     *
     * @param parent the parent node
     * @todo move propertyListTable initialization someplace else?
     */
    public FObj(FONode parent) {
        super(parent);
        
        // determine if isOutOfLineFODescendant should be set
        if (parent != null && parent instanceof FObj) {
            if (((FObj)parent).getIsOutOfLineFODescendant() == true) {
                isOutOfLineFODescendant = true;
            } else {
                int foID = getNameId();
                if (foID == FO_FLOAT || foID == FO_FOOTNOTE
                    || foID == FO_FOOTNOTE_BODY) {
                        isOutOfLineFODescendant = true;
                }
            }
        }
        
        if (propertyListTable == null) {
            propertyListTable = new PropertyMaker[Constants.PROPERTY_COUNT+1];
            PropertyMaker[] list = FOPropertyMapping.getGenericMappings();
            for (int i = 1; i < list.length; i++) {
                if (list[i] != null)
                    propertyListTable[i] = list[i]; 
            }
        }
    }

    /**
     * @see org.apache.fop.fo.FONode#processNode
     */
    public void processNode(String elementName, Locator locator, 
                            Attributes attlist, PropertyList pList) throws SAXParseException {
        setLocator(locator);
        propertyList.addAttributesToList(attlist);
        propertyList.setWritingMode();
        bind(propertyList);
        propMgr = new PropertyManager(propertyList);
    }

    /**
     * Create a default property list for this element. 
     */
    protected PropertyList createPropertyList(PropertyList parent, FOEventHandler foEventHandler) throws SAXParseException {
        //return foEventHandler.getPropertyListMaker().make(this, parent);
        propertyList = new StaticPropertyList(this, parent);
        return propertyList;
    }

    /**
     * Bind property values from the property list to the FO node.
     * Must be overriden in all FObj subclasses. 
     * @param pList the PropertyList where the properties can be found.
     * @throws SAXParseException
     */
    public void bind(PropertyList pList) throws SAXParseException {
        throw new SAXParseException("Unconverted element " + this, locator);
    }

    /**
     * Setup the id for this formatting object.
     * Most formatting objects can have an id that can be referenced.
     * This methods checks that the id isn't already used by another
     * fo and sets the id attribute of this object.
     */
     protected void checkId(String id) throws SAXParseException {
        if (!id.equals("")) {
            Set idrefs = getFOEventHandler().getIDReferences();
            if (!idrefs.contains(id)) {
                idrefs.add(id);
            } else {
                throw new SAXParseException("Property id \"" + id + 
                        "\" previously used; id values must be unique" +
                        " in document.", locator);
            }
        }
    }

    /**
     * Returns Out Of Line FO Descendant indicator.
     * @return true if Out of Line FO or Out Of Line descendant, false otherwise
     */
    public boolean getIsOutOfLineFODescendant() {
        return isOutOfLineFODescendant;
    }

    /**
     * Return the PropertyManager object for this FO.  PropertyManager
     * tends to hold the traits for this FO, and is primarily used in layout.
     * @return the property manager for this FO
     */
    public PropertyManager getPropertyManager() {
        return propMgr;
    }

    /**
     * Return the property list object for this FO.  PropertyList tends
     * to hold the base, pre-trait properties for this FO, either explicitly
     * declared in the input XML or from inherited values.
     */
    public PropertyList getPropertyList() {
        return propertyList;
    }

    /**
     * Helper method to quickly obtain the value of a property
     * for this FO, without querying for the propertyList first.
     * @param propId - the Constants ID of the desired property to obtain
     * @return the property
     */
    public Property getProperty(int propId) {
        return propertyList.get(propId);
    }

    /**
     * Convenience method to quickly obtain the String value of a property
     * for this FO, without querying for the propertyList first.
     * Meaningful only for properties having a string representation
     * @param propId - the Constants ID of the desired property to obtain
     * @return the String value of the property value
     */
    public String getPropString(int propId) {
        return propertyList.get(propId).getString();
    }

    /**
     * Convenience method to quickly obtain the length value of a property
     * for this FO, without querying for the propertyList first.
     * Meaningful only for properties having a length representation
     * Note: getValue() only correct after resolution completed, therefore
     * should be called only in layout manager code.
     * @param propId - the Constants ID of the desired property to obtain
     * @return the length value of the property value
     */
    public int getPropLength(int propId) {
        return propertyList.get(propId).getLength().getValue();
    }

    /**
     * Convenience method to quickly obtain the Constants class enumeration
     * value of a property for this FO.  Meaningful only for properties
     * having an enumeration representation
     * @param propId - the Constants ID of the desired property to obtain
     * @return the enumeration value of the property value
     */
    public int getPropEnum(int propId) {
        return propertyList.get(propId).getEnum();
    }

    /**
     * @see org.apache.fop.fo.FONode#addChildNode(FONode)
     */
    protected void addChildNode(FONode child) throws SAXParseException {
        if (PropertySets.canHaveMarkers(getNameId()) && 
            child.getNameId() == FO_MARKER) {
                addMarker((Marker) child);
        } else {
            if (childNodes == null) {
                childNodes = new ArrayList();
            }
            childNodes.add(child);
        }
    }

    /**
     * Find the nearest parent, grandparent, etc. FONode that is also an FObj
     * @return FObj the nearest ancestor FONode that is an FObj
     */
    public FObj findNearestAncestorFObj() {
        FONode par = parent;
        while (par != null && !(par instanceof FObj)) {
            par = par.parent;
        }
        return (FObj) par;
    }

    /* This section is the implemenation of the property context. */

    /**
     * Assign the size of a layout dimension to the key. 
     * @param key the Layout dimension, from PercentBase.
     * @param dimension The layout length.
     */
    public void setLayoutDimension(Integer key, int dimension) {
        if (layoutDimension == null){
            layoutDimension = new HashMap();
        }
        layoutDimension.put(key, new Integer(dimension));
    }
    
    /**
     * Assign the size of a layout dimension to the key. 
     * @param key the Layout dimension, from PercentBase.
     * @param dimension The layout length.
     */
    public void setLayoutDimension(Integer key, float dimension) {
        if (layoutDimension == null){
            layoutDimension = new HashMap();
        }
        layoutDimension.put(key, new Float(dimension));
    }
    
    /**
     * Return the size associated with the key.
     * @param key The layout dimension key.
     * @return the length.
     */
    public Number getLayoutDimension(Integer key) {
        if (layoutDimension != null) {
            Number result = (Number) layoutDimension.get(key);
            if (result != null) {
                return result;
            }
        }
        if (parent != null) {
            return ((FObj) parent).getLayoutDimension(key);
        }
        return new Integer(0);
    }

    /**
     * Check if this formatting object generates reference areas.
     * @return true if generates reference areas
     * @todo see if needed
     */
    public boolean generatesReferenceAreas() {
        return false;
    }

    /**
     * @see org.apache.fop.fo.FONode#getChildNodes()
     */
    public ListIterator getChildNodes() {
        if (childNodes != null) {
            return childNodes.listIterator();
        }
        return null;
    }

    /**
     * Return an iterator over the object's childNodes starting
     * at the passed-in node.
     * @param childNode First node in the iterator
     * @return A ListIterator or null if childNode isn't a child of
     * this FObj.
     */
    public ListIterator getChildNodes(FONode childNode) {
        if (childNodes != null) {
            int i = childNodes.indexOf(childNode);
            if (i >= 0) {
                return childNodes.listIterator(i);
            }
        }
        return null;
    }

   /**
     * Add the marker to this formatting object.
     * If this object can contain markers it checks that the marker
     * has a unique class-name for this object and that it is
     * the first child.
     * @param marker Marker to add.
     */
    protected void addMarker(Marker marker) {
        String mcname = marker.getPropString(PR_MARKER_CLASS_NAME);
        if (childNodes != null) {
            // check for empty childNodes
            for (Iterator iter = childNodes.iterator(); iter.hasNext();) {
                FONode node = (FONode)iter.next();
                if (node instanceof FOText) {
                    FOText text = (FOText)node;
                    if (text.willCreateArea()) {
                        getLogger().error("fo:marker must be an initial child: " + mcname);
                        return;
                    } else {
                        iter.remove();
                    }
                } else {
                    getLogger().error("fo:marker must be an initial child: " + mcname);
                    return;
                }
            }
        }
        if (markers == null) {
            markers = new HashMap();
        }
        if (!markers.containsKey(mcname)) {
            markers.put(mcname, marker);
        } else {
            getLogger().error("fo:marker 'marker-class-name' "
                    + "must be unique for same parent: " + mcname);
        }
    }

    /**
     * @return true if there are any Markers attached to this object
     */
    public boolean hasMarkers() {
        return markers != null && !markers.isEmpty();
    }

    /**
     * @return th collection of Markers attached to this object
     */
    public Map getMarkers() {
        return markers;
    }

    /*
     * Return a string representation of the fo element.
     * Deactivated in order to see precise ID of each fo element created
     *    (helpful for debugging)
     */
/*    public String toString() {
        return getName() + " at line " + line + ":" + column;
    }
*/    

    /**
     * Convenience method for validity checking.  Checks if the
     * incoming node is a member of the "%block;" parameter entity
     * as defined in Sect. 6.2 of the XSL 1.0 & 1.1 Recommendations
     * @param nsURI namespace URI of incoming node
     * @param lName local name (i.e., no prefix) of incoming node 
     * @return true if a member, false if not
     */
    protected boolean isBlockItem(String nsURI, String lName) {
        return (nsURI == FO_URI && 
            (lName.equals("block") 
            || lName.equals("table") 
            || lName.equals("table-and-caption") 
            || lName.equals("block-container")
            || lName.equals("list-block") 
            || lName.equals("float")
            || isNeutralItem(nsURI, lName)));
    }

    /**
     * Convenience method for validity checking.  Checks if the
     * incoming node is a member of the "%inline;" parameter entity
     * as defined in Sect. 6.2 of the XSL 1.0 & 1.1 Recommendations
     * @param nsURI namespace URI of incoming node
     * @param lName local name (i.e., no prefix) of incoming node 
     * @return true if a member, false if not
     */
    protected boolean isInlineItem(String nsURI, String lName) {
        return (nsURI == FO_URI && 
            (lName.equals("bidi-override") 
            || lName.equals("character") 
            || lName.equals("external-graphic") 
            || lName.equals("instream-foreign-object")
            || lName.equals("inline") 
            || lName.equals("inline-container")
            || lName.equals("leader") 
            || lName.equals("page-number") 
            || lName.equals("page-number-citation")
            || lName.equals("basic-link")
            || (lName.equals("multi-toggle")
                && (getNameId() == FO_MULTI_CASE || findAncestor(FO_MULTI_CASE) > 0))
            || (lName.equals("footnote") && !isOutOfLineFODescendant)
            || isNeutralItem(nsURI, lName)));
    }

    /**
     * Convenience method for validity checking.  Checks if the
     * incoming node is a member of the "%block;" parameter entity
     * or "%inline;" parameter entity
     * @param nsURI namespace URI of incoming node
     * @param lName local name (i.e., no prefix) of incoming node 
     * @return true if a member, false if not
     */
    protected boolean isBlockOrInlineItem(String nsURI, String lName) {
        return (isBlockItem(nsURI, lName) || isInlineItem(nsURI, lName));
    }

    /**
     * Convenience method for validity checking.  Checks if the
     * incoming node is a member of the neutral item list
     * as defined in Sect. 6.2 of the XSL 1.0 & 1.1 Recommendations
     * @param nsURI namespace URI of incoming node
     * @param lName local name (i.e., no prefix) of incoming node 
     * @return true if a member, false if not
     */
    protected boolean isNeutralItem(String nsURI, String lName) {
        return (nsURI == FO_URI && 
            (lName.equals("multi-switch") 
            || lName.equals("multi-properties")
            || lName.equals("wrapper") 
            || (!isOutOfLineFODescendant && lName.equals("float"))
            || lName.equals("retrieve-marker")));
    }
    
    /**
     * Convenience method for validity checking.  Checks if the
     * current node has an ancestor of a given name.
     * @param ancestorID -- Constants ID of node name to check for (e.g., FO_ROOT)
     * @return number of levels above FO where ancestor exists, 
     *    -1 if not found
     */
    protected int findAncestor(int ancestorID) {
        int found = 1;
        FONode temp = getParent();
        while (temp != null) {
            if (temp.getNameId() == ancestorID) {
                return found;
            }
            found += 1;
            temp = temp.getParent();
        }
        return -1;
    }
}


// InvalidDateException.java
// $Id: InvalidDateException.java,v 1.1 2000/09/26 13:54:04 bmahe Exp $
// (c) COPYRIGHT MIT, INRIA and Keio, 2000.
// Please first read the full copyright statement in file COPYRIGHT.html
package org.w3c.util;

/**
 * @author Benoit Mahi (bmahe@w3.org)
 * @version $Revision: 1.1 $
 */
public class InvalidDateException extends Exception {

    private static final long serialVersionUID = -9012791102239300978L;

    public InvalidDateException(String msg) {
        super(msg);
    }

}

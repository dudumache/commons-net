package org.apache.commons.net.ftp;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Commons" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.util.Vector;

/**
 * FTPFileIterator.java
 * This class implements a bidirectional iterator over an FTPFileList.
 * Elements may be retrieved one at at time using the hasNext() - next()
 * syntax familiar from Java 2 collections.  Alternatively, entries may
 * be receieved as an array of any requested number of entries or all of them.
 * 
 * @author <a href="mailto:scohen@apache.org">Steve Cohen</a>
 * @version $Id: FTPFileIterator.java,v 1.7 2004/01/09 09:07:03 dfs Exp $
 * @see org.apache.commons.net.ftp.FTPFileList
 * @see org.apache.commons.net.ftp.FTPFileEntryParser
 */
public abstract class FTPFileIterator
{
    /**
     * Returns a list of FTPFile objects for ALL files listed in the server's
     * LIST output.
     *
     * @return a list of FTPFile objects for ALL files listed in the server's
     * LIST output.
     */
    public abstract FTPFile[] getFiles();

    /**
     * Returns an array of at most <code>quantityRequested</code> FTPFile 
     * objects starting at this iterator's current position  within its 
     * associated list. If fewer than <code>quantityRequested</code> such 
     * elements are available, the returned array will have a length equal 
     * to the number of entries at and after after the current position.  
     * If no such entries are found, this array will have a length of 0.
     * 
     * After this method is called the current position is advanced by 
     * either <code>quantityRequested</code> or the number of entries 
     * available after the iterator, whichever is fewer.
     * 
     * @param quantityRequested
     * the maximum number of entries we want to get.  A 0
     * passed here is a signal to get ALL the entries.
     * 
     * @return an array of at most <code>quantityRequested</code> FTPFile 
     * objects starting at the current position of this iterator within its 
     * list and at least the number of elements which  exist in the list at 
     * and after its current position.
     */
    public abstract FTPFile[] getNext(int quantityRequested);


    /**
     * Method for determining whether getNext() will successfully return a
     * non-null value.
     *
     * @return true if there exist any files after the one currently pointed
     * to by the internal iterator, false otherwise.
     */
    public abstract boolean hasNext();

    /**
     * Returns a single parsed FTPFile object corresponding to the raw input
     * line at this iterator's current position.
     *
     * After this method is called the internal iterator is advanced by one
     * element (unless already at end of list).
     *
     * @return a single FTPFile object corresponding to the raw input line
     * at the position of the internal iterator over the list of raw input
     * lines maintained by this object or null if no such object exists.
     */
    public abstract FTPFile next();

    /**
     * Returns an array of at most <code>quantityRequested</code> FTPFile 
     * objects starting at the position preceding this iterator's current 
     * position within its associated list. If fewer than 
     * <code>quantityRequested</code> such elements are available, the 
     * returned array will have a length equal to the number of entries after
     * the iterator.  If no such entries are found, this array will have a 
     * length of 0.  The entries will be ordered in the same order as the 
     * list, not reversed.
     *
     * After this method is called the current position is moved back by 
     * either <code>quantityRequested</code> or the number of entries 
     * available before the current position, whichever is fewer.
     * @param quantityRequested the maximum number of entries we want to get.  
     * A 0 passed here is a signal to get ALL the entries.
     * @return  an array of at most <code>quantityRequested</code> FTPFile 
     * objects starting at the position preceding the current position of 
     * this iterator within its list and at least the number of elements which
     * exist in the list prior to its current position.
     */
    public abstract FTPFile[] getPrevious(int quantityRequested);

    /**
     * Method for determining whether getPrevious() will successfully return a
     * non-null value.
     *
     * @return true if there exist any files before the one currently pointed
     * to by the internal iterator, false otherwise.
     */
    public abstract boolean hasPrevious();

    /**
     * Returns a single parsed FTPFile object corresponding to the raw input
     * line at the position preceding that of the internal iterator over
     * the list of raw lines maintained by this object
     *
     * After this method is called the internal iterator is retreated by one
     * element (unless it is already at beginning of list).
     * @return a single FTPFile object corresponding to the raw input line
     * at the position immediately preceding that of the internal iterator
     * over the list of raw input lines maintained by this object.
     */
    public abstract FTPFile previous();
}


/* Emacs configuration
 * Local variables:        **
 * mode:             java  **
 * c-basic-offset:   4     **
 * indent-tabs-mode: nil   **
 * End:                    **
 */

/*
 * Copyright (c) 2019 Livio, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the Livio Inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.smartdevicelink.managers.file;


import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.managers.file.filetypes.SdlFile;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.FileUtls;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * <strong>FileManager</strong> <br>
 * <p>
 * Note: This class must be accessed through the SdlManager. Do not instantiate it by itself. <br>
 * <p>
 * The SDLFileManager uploads files and keeps track of all the uploaded files names during a session. <br>
 * <p>
 * We need to add the following struct: SDLFile<br>
 * <p>
 * It is broken down to these areas: <br>
 * <p>
 * 1. Getters <br>
 * 2. Deletion methods <br>
 * 3. Uploading Files / Artwork
 */
public class FileManager extends BaseFileManager {

    /**
     * Constructor for FileManager
     *
     * @param internalInterface an instance of the ISdl interface that can be used for common SDL operations (sendRpc, addRpcListener, etc)
     * @param fileManagerConfig an instance of the FileManagerConfig gives access to artworkRetryCount and fileRetryCount to let us if those file types can be re-upload if they fail
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public FileManager(ISdl internalInterface, FileManagerConfig fileManagerConfig) {
        // setup
        super(internalInterface, fileManagerConfig);
    }

    /**
     * Creates and returns a PutFile request that would upload a given SdlFile
     *
     * @param file SdlFile with fileName and one of A) fileData, B) Uri, or C) resourceID set
     * @return a valid PutFile request if SdlFile contained a fileName and sufficient data
     */
    @Override
    PutFile createPutFile(@NonNull final SdlFile file) {
        PutFile putFile = new PutFile();
        if (file.getName() == null) {
            throw new IllegalArgumentException("You must specify an file name in the SdlFile");
        } else {
            putFile.setSdlFileName(file.getName());
        }

        if (file.getFilePath() != null) {
            //Attempt to access the file via a path
            byte[] data = FileUtls.getFileData(file.getFilePath());
            if (data != null) {
                putFile.setFileData(data);
            } else {
                throw new IllegalArgumentException("File at path was empty");
            }
        } else if (file.getURI() != null) {
            // Use URI to upload file
            byte[] data = contentsOfUri(file.getURI());
            if (data != null) {
                putFile.setFileData(data);
            } else {
                throw new IllegalArgumentException("Uri was empty");
            }
        } else if (file.getFileData() != null) {
            // Use file data (raw bytes) to upload file
            putFile.setFileData(file.getFileData());
        } else {
            throw new IllegalArgumentException("The SdlFile to upload does " +
                    "not specify its resourceId, Uri, or file data");
        }

        if (file.getType() != null) {
            putFile.setFileType(file.getType());
        }
        putFile.setPersistentFile(file.isPersistent());

        return putFile;
    }


    /**
     * Helper method to take Uri and turn it into byte array
     *
     * @param uri Uri for desired file
     * @return Resulting byte array
     */
    private byte[] contentsOfUri(URI uri) {
        InputStream is = null;
        try {
            is = uri.toURL().openStream();
            return contentsOfInputStream(is);
        } catch (IOException e) {
            DebugTool.logError(TAG, "Can't read from URI", e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

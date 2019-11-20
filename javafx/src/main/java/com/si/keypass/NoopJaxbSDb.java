package com.si.keypass;

import org.apache.commons.codec.binary.Base64;
import org.linguafranca.pwdb.kdbx.SerializableDatabase;
import org.linguafranca.pwdb.kdbx.StreamEncryptor;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbSerializableDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbGroupBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.KeePassFile;
import org.linguafranca.pwdb.kdbx.jaxb.binding.StringField;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @see #load(InputStream)
 * @see #save(OutputStream)
 */
public class NoopJaxbSDb implements SerializableDatabase {
    protected KeePassFile keePassFile;
    private StreamEncryptor encryption;

    public KeePassFile getKeePassFile() {
        return keePassFile;
    }

    public void setKeePassFile(KeePassFile keePassFile) {
        this.keePassFile = keePassFile;
    }

    @Override
    public StreamEncryptor getEncryption() {
        return null;
    }

    @Override
    public void setEncryption(StreamEncryptor encryption) {

    }

    @Override
    public byte[] getHeaderHash() {
        return new byte[0];
    }

    @Override
    public void setHeaderHash(byte[] hash) {

    }

    @Override
    public void addBinary(int index, byte[] payload) {

    }

    @Override
    public SerializableDatabase load(InputStream inputStream) {
        try {
            JAXBContext jc = JAXBContext.newInstance(KeePassFile.class);
            Unmarshaller u = jc.createUnmarshaller();
            u.setListener(new Unmarshaller.Listener() {
                @Override
                public void afterUnmarshal(Object target, Object parent) {
                    try {
                        if (target instanceof StringField.Value) {
                            StringField.Value value = (StringField.Value) target;
                            if (value.getProtected() !=null && value.getProtected()) {
                                byte[] encrypted = Base64.decodeBase64(value.getValue().getBytes());
                                String decrypted = new String(encryption.decrypt(encrypted), "UTF-8");
                                value.setValue(decrypted);
                                value.setProtected(false);
                            }
                        }
                        if (target instanceof JaxbGroupBinding && (parent instanceof JaxbGroupBinding)) {
                            ((JaxbGroupBinding) target).parent = ((JaxbGroupBinding) parent);
                        }
                        if (target instanceof JaxbEntryBinding && (parent instanceof JaxbGroupBinding)) {
                            ((JaxbEntryBinding) target).parent = ((JaxbGroupBinding) parent);
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException();
                    }
                }
            });
            keePassFile = (KeePassFile) u.unmarshal(inputStream);
            return this;
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(KeePassFile.class);
            Marshaller u = jc.createMarshaller();
            u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            u.setListener(new Marshaller.Listener() {
                @Override
                public void beforeMarshal(Object source) {
                    try {
                        if (source instanceof StringField) {
                            StringField field = (StringField) source;
                            StringField.Value value = field.getValue();
                            if (value.getProtected() !=null && value.getProtected()) {
                                // TODO, needs to be unecrypted?
                                String svalue = value.getValue();
                            }
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException();
                    }
                }
            });
            u.marshal(keePassFile, outputStream);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

}

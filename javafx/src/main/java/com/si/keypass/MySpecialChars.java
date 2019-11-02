package com.si.keypass;

import org.passay.CharacterData;

/**
 * A more limited version of special chars
 * @see org.passay.EnglishCharacterData#Special
 */
public class MySpecialChars implements CharacterData {
    public static final CharacterData INSTANCE = new MySpecialChars();
    @Override
    public String getErrorCode() {
        return "INSUFFICIENT_SPECIAL";
    }

    @Override
    public String getCharacters() {
        return "!@#$%+";
    }
}

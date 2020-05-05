/*
 * Test.java
 *
 * APRON Library / Java Apron binding
 *
 * Copyright (C) Antoine Mine' 2010
 */

package apron;

import java.io.*;
import java.util.Arrays;
import gmp.*;
import org.junit.jupiter.api.Test;

/**
 * Simple test for the Java Apron binding.
 *
 * <p>
 * Run with: java -ea -esa apron.Test
 */
public class CustomTest {

	/* Serialization test */
	/* ------------------ */

	public Object testSerialize(Object o) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream jout = new ObjectOutputStream(out);
			jout.writeObject(o);
			jout.close();
			System.out.println("OUT: " + o);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			ObjectInputStream jin = new ObjectInputStream(in);
			Object oo = jin.readObject();
			System.out.println("IN : " + oo);
			jin.close();
			return oo;
		} catch (Exception e) {
			System.out.println("caught exception " + e);
			return null;
		}
	}

	/* Abstract domain testing */
	/* ------------------------ */

	public void testDomain(Manager man) throws ApronException {
        System.out.println("Hello");
    }

    @Test
	public void variousTests() throws ApronException, CloneNotSupportedException {
        String[] ints = {"a", "b"};
        String[] reals = {};
        Environment env = new Environment(ints, reals);
        Manager man = new Polka(true);

        Linterm1 a = new Linterm1("a", new MpqScalar(1));
        Linterm1 b = new Linterm1("b", new MpqScalar(1));

        Linterm1[] linterms = {a, b};

        Linexpr1 e = new Linexpr1(env, linterms, new MpqScalar(1));

        Lincons1 c = new Lincons1(Lincons1.EQ, e);

        Lincons1[] cons = {c};

        System.out.println(c.toString());

        Abstract1 a1 = new Abstract1(man, cons);

        System.out.println(a1.toString());

        
        //Linexpr1 xlinexp2 = new Linexpr1(env, xltrms2, new MpqScalar(2));
    }
}

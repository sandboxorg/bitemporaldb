package org.bitemporal

import java.util.Date

import org.bitemporal.domain.Student
import org.scalatest.{FlatSpec, Matchers}

import com.google.gson.Gson;

/*
 * Store two temporal versions of the same object to the database.
 * Then find one version by date and update its logical properties.
 *
 * This should result in a new technical version of the updated temporal version.
 *
 * Use Case:
 * + A person has the name "Allen Doe" from Date d1 to Date d2
 * + The person marries on d3 and changes his name to "Allen Dot"
 * + We find out that the original name should have been "Allen Doet", and record this to the database.
 */

class UpdateTemporalTest extends FlatSpec with Matchers {

    MemoryDb.clearDatabase()

    val s  = new Student("Allen", "Doe")
    val t  = new Student("Allen", "Dot")

    val sPeriod = new Period(TestData.d1, TestData.d2);

    // save first temporal version
    val sId = MemoryDb.store(s, sPeriod);

    val template = new Student()

    // save the other temporal version
    MemoryDb.updateLogical(sId, t, new Period(TestData.d3, TestData.d4))
    val context1 =  new BitemporalContext(new Date(), TestData.d1)
    val retrieved1: Temporal[Student] = MemoryDb.findLogical(template, sId, context1).get

    MemoryDb.updateLogical(sId, new Student("John", "Doe"), sPeriod)

    // since we are searching with the old context (the transaction time before we did the update),
    // we are expecting the old version of the Student.
    val retrieved2 = MemoryDb.findLogical(template, sId, context1).get
    println(new Gson().toJson(retrieved1.element))
    println(new Gson().toJson(retrieved2.element))
    retrieved2.element should equal (retrieved1.element)

    // search with the new transaction time. Thus we expect to find the correct/updated name.
    val retrieved3 : Temporal[Student] = MemoryDb.findLogical(template, sId, new BitemporalContext(new Date(), TestData.d1)).get;
    retrieved3.element.firstName should be ("John")
}


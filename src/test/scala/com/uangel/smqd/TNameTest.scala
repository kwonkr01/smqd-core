package t2x.smqd

import org.scalatest.FlatSpec

/**
  * 2018. 5. 31. - Created by Kwon, Yeong Eon
  */
class TNameTest extends FlatSpec {

  "TName and TPath" should "empty" in {
    val ns = TName.parse("")
    assert(ns.size == 1)
    assert(ns.head.name == "")
  }

  it should "/" in {
    val ns = TName.parse("/")
    assert(ns.size == 2)
    assert(ns(0).name == "")
    assert(ns(1).name == "")
  }

  it should "//" in {
    val ns = TName.parse("//")
    assert(ns.size == 3)
    assert(ns(0).name == "")
    assert(ns(1).name == "")
    assert(ns(2).name == "")

    val fp = TPath.parseForFilter(ns)
    assert(fp.isDefined)
    assert(fp.get.toString == "//")
  }

  it should "one" in {
    val ns = TName.parse("one")
    assert(ns.size == 1)
    assert(ns(0).name == "one")
  }

  it should "one/" in {
    val ns = TName.parse("one/")
    assert(ns.size == 2)
    assert(ns(0).name == "one")
    assert(ns(1).name == "")
  }

  it should "/one" in {
    val ns = TName.parse("/one")
    assert(ns.size == 2)
    assert(ns.head.name == "")
    assert(ns.last.name == "one")
  }

  it should "one/two/three" in {
    val ns = TName.parse("one/two/three")
    assert(ns.size == 3)
    assert(ns(0).name == "one")
    assert(ns(1).name == "two")
    assert(ns(2).name == "three")
  }

  it should "one/two/three/" in {
    val ns = TName.parse("one/two/three/")
    assert(ns.size == 4)
    assert(ns(0).name == "one")
    assert(ns(1).name == "two")
    assert(ns(2).name == "three")
    assert(ns(3).name == "")
  }

  it should "/one/two/three/" in {
    val ns = TName.parse("/one/two/three/")
    assert(ns.size == 5)
    assert(ns(0).name == "")
    assert(ns(1).name == "one")
    assert(ns(2).name == "two")
    assert(ns(3).name == "three")
    assert(ns(4).name == "")
  }

  it should "one//three/" in {
    val ns = TName.parse("one//three/")
    assert(ns.size == 4)
    assert(ns(0).name == "one")
    assert(ns(1).name == "")
    assert(ns(2).name == "three")
    assert(ns(3).name == "")
  }

  it should "#" in {
    val ns = TName.parse("#")
    assert(ns.size == 1)
    assert(ns(0).name == "#")
  }

  it should "one/two/#" in {
    val ns = TName.parse("one/two/#")
    assert(ns.size == 3)
    assert(ns(2).isInstanceOf[TNameMultiWildcard])

    val tp = TPath.parseForTopic(ns)
    assert(tp.isEmpty)

    val fl = TPath.parseForFilter(ns)
    assert(fl.isDefined)
  }

  it should "one/+/#" in {
    val path = "one/+/#"
    val ns = TName.parse(path)
    assert(ns.size == 3)
    assert(ns(1).isInstanceOf[TNameSingleWildcard])
    assert(ns(2).isInstanceOf[TNameMultiWildcard])

    val tp = TPath.parseForTopic(ns)
    assert(tp.isEmpty)

    val fl = TPath.parseForFilter(ns)
    assert(fl.isDefined)
    assert(fl.get.toString == path)
  }

  it should "one/#/" in {
    val ns = TName.parse("one/#/")
    assert(ns.size == 3)
    assert(ns(1).isInstanceOf[TNameInvalid])

    val tp = TPath.parseForTopic(ns)
    assert(tp.isEmpty)

    val fl = TPath.parseForFilter(ns)
    assert(fl.isEmpty)
  }

  it should "+/+" in {
    val ns = TName.parse("+/+")
    assert(ns.size == 2)
    assert(ns(0).isInstanceOf[TNameSingleWildcard])
    assert(ns(1).isInstanceOf[TNameSingleWildcard])

    val fl = TPath.parseForFilter(ns)
    assert(fl.isDefined)
  }

  "Local subscription path" should "parse" in {
    val fl = TPath.parseForFilter("$local/topic")
    assert(fl.isDefined)
    val f = fl.get
    assert(f.prefix == FilterPathPrefix.Local, "should be local")

    val tl = TPath.parseForTopic("topic")
    assert(tl.isDefined)
    val t = tl.get

    assert(f.matchFor(t), "local match")
    assert(f.toString == "$local/topic")
  }

  "Local subscription path with $SYS" should "parse" in {
    val fl = TPath.parseForFilter("$local/$SYS/topic")
    assert(fl.isDefined)
    val f = fl.get
    assert(f.prefix == FilterPathPrefix.Local, "should be local")

    val tl = TPath.parseForTopic("$SYS/topic")
    assert(tl.isDefined)
    val t = tl.get

    assert(f.matchFor(t), "local $SYS match")
    assert(f.toString == "$local/$SYS/topic")
  }

  "Queue subscription path" should "parse" in {
    val fl = TPath.parseForFilter("$queue/topic")
    assert(fl.isDefined)
    val f = fl.get
    assert(f.prefix == FilterPathPrefix.Queue, "should be queue")

    val tl = TPath.parseForTopic("topic")
    assert(tl.isDefined)
    val t = tl.get

    assert(f.matchFor(t), "queue match")
    assert(f.toString == "$queue/topic")
  }

  "Shared subscription path" should "parse" in {
    val fl = TPath.parseForFilter("$share/group/topic")
    assert(fl.isDefined)
    val f = fl.get
    assert(f.prefix == FilterPathPrefix.Share, "should be share")
    assert(f.group.isDefined)
    assert(f.group.get == "group")

    val tl = TPath.parseForTopic("topic")
    assert(tl.isDefined)
    val t = tl.get

    assert(f.matchFor(t), "share match")
    assert(f.toString == "$share/group/topic")
  }

  "Implicit conversion from string" should "parse" in {
    val fp: FilterPath = "$share/groups/sensors/+/works/#"

    val tp: TopicPath = "sensors/123/works/129"
  }
}
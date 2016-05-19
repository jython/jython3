import base64
import datetime
import gc
import sys
import time
import unittest
import xmlrpc.client
import xmlrpc.server
import mimetools
import http.client
import socket
import io
import os
import re
from test import support

try:
    import threading
except ImportError:
    threading = None

try:
    str
except NameError:
    have_unicode = False
else:
    have_unicode = True

if support.is_jython:
    import _socket
    _socket._NUM_THREADS = 5

alist = [{'astring': 'foo@bar.baz.spam',
          'afloat': 7283.43,
          'anint': 2**20,
          'ashortlong': 2,
          'anotherlist': ['.zyx.41'],
          'abase64': xmlrpc.client.Binary("my dog has fleas"),
          'boolean': xmlrpc.client.False,
          'unicode': '\u4000\u6000\u8000',
          'ukey\u4000': 'regular value',
          'datetime1': xmlrpc.client.DateTime('20050210T11:41:23'),
          'datetime2': xmlrpc.client.DateTime(
                        (2005, 0o2, 10, 11, 41, 23, 0, 1, -1)),
          'datetime3': xmlrpc.client.DateTime(
                        datetime.datetime(2005, 0o2, 10, 11, 41, 23)),
          }]

class XMLRPCTestCase(unittest.TestCase):

    def test_dump_load(self):
        self.assertEqual(alist,
                         xmlrpc.client.loads(xmlrpc.client.dumps((alist,)))[0][0])

    def test_dump_bare_datetime(self):
        # This checks that an unwrapped datetime.date object can be handled
        # by the marshalling code.  This can't be done via test_dump_load()
        # since with use_datetime set to 1 the unmarshaller would create
        # datetime objects for the 'datetime[123]' keys as well
        dt = datetime.datetime(2005, 0o2, 10, 11, 41, 23)
        s = xmlrpc.client.dumps((dt,))
        (newdt,), m = xmlrpc.client.loads(s, use_datetime=1)
        self.assertEqual(newdt, dt)
        self.assertEqual(m, None)

        (newdt,), m = xmlrpc.client.loads(s, use_datetime=0)
        self.assertEqual(newdt, xmlrpc.client.DateTime('20050210T11:41:23'))

    def test_datetime_before_1900(self):
        # same as before but with a date before 1900
        dt = datetime.datetime(1, 0o2, 10, 11, 41, 23)
        s = xmlrpc.client.dumps((dt,))
        (newdt,), m = xmlrpc.client.loads(s, use_datetime=1)
        self.assertEqual(newdt, dt)
        self.assertEqual(m, None)

        (newdt,), m = xmlrpc.client.loads(s, use_datetime=0)
        self.assertEqual(newdt, xmlrpc.client.DateTime('00010210T11:41:23'))

    def test_cmp_datetime_DateTime(self):
        now = datetime.datetime.now()
        dt = xmlrpc.client.DateTime(now.timetuple())
        self.assertTrue(dt == now)
        self.assertTrue(now == dt)
        then = now + datetime.timedelta(seconds=4)
        self.assertTrue(then >= dt)
        self.assertTrue(dt < then)

    def test_bug_1164912 (self):
        d = xmlrpc.client.DateTime()
        ((new_d,), dummy) = xmlrpc.client.loads(xmlrpc.client.dumps((d,),
                                            methodresponse=True))
        self.assertIsInstance(new_d.value, str)

        # Check that the output of dumps() is still an 8-bit string
        s = xmlrpc.client.dumps((new_d,), methodresponse=True)
        self.assertIsInstance(s, str)

    def test_newstyle_class(self):
        class T(object):
            pass
        t = T()
        t.x = 100
        t.y = "Hello"
        ((t2,), dummy) = xmlrpc.client.loads(xmlrpc.client.dumps((t,)))
        self.assertEqual(t2, t.__dict__)

    def test_dump_big_long(self):
        self.assertRaises(OverflowError, xmlrpc.client.dumps, (2**99,))

    def test_dump_bad_dict(self):
        self.assertRaises(TypeError, xmlrpc.client.dumps, ({(1, 2, 3): 1},))

    def test_dump_recursive_seq(self):
        l = [1, 2, 3]
        t = [3, 4, 5, l]
        l.append(t)
        self.assertRaises(TypeError, xmlrpc.client.dumps, (l,))

    def test_dump_recursive_dict(self):
        d = {'1':1, '2':1}
        t = {'3':3, 'd':d}
        d['t'] = t
        self.assertRaises(TypeError, xmlrpc.client.dumps, (d,))

    def test_dump_big_int(self):
        if sys.maxsize > 2**31-1:
            self.assertRaises(OverflowError, xmlrpc.client.dumps,
                              (int(2**34),))

        xmlrpc.client.dumps((xmlrpc.client.MAXINT, xmlrpc.client.MININT))
        self.assertRaises(OverflowError, xmlrpc.client.dumps, (xmlrpc.client.MAXINT+1,))
        self.assertRaises(OverflowError, xmlrpc.client.dumps, (xmlrpc.client.MININT-1,))

        def dummy_write(s):
            pass

        m = xmlrpc.client.Marshaller()
        m.dump_int(xmlrpc.client.MAXINT, dummy_write)
        m.dump_int(xmlrpc.client.MININT, dummy_write)
        self.assertRaises(OverflowError, m.dump_int, xmlrpc.client.MAXINT+1, dummy_write)
        self.assertRaises(OverflowError, m.dump_int, xmlrpc.client.MININT-1, dummy_write)


    def test_dump_none(self):
        value = alist + [None]
        arg1 = (alist + [None],)
        strg = xmlrpc.client.dumps(arg1, allow_none=True)
        self.assertEqual(value,
                         xmlrpc.client.loads(strg)[0][0])
        self.assertRaises(TypeError, xmlrpc.client.dumps, (arg1,))

    @unittest.skipIf(support.is_jython, "FIXME: #1875 not working in Jython")
    def test_default_encoding_issues(self):
        # SF bug #1115989: wrong decoding in '_stringify'
        utf8 = """<?xml version='1.0' encoding='iso-8859-1'?>
                  <params>
                    <param><value>
                      <string>abc \x95</string>
                      </value></param>
                    <param><value>
                      <struct>
                        <member>
                          <name>def \x96</name>
                          <value><string>ghi \x97</string></value>
                          </member>
                        </struct>
                      </value></param>
                  </params>
                  """

        # sys.setdefaultencoding() normally doesn't exist after site.py is
        # loaded.  Import a temporary fresh copy to get access to it
        # but then restore the original copy to avoid messing with
        # other potentially modified sys module attributes
        old_encoding = sys.getdefaultencoding()
        with support.CleanImport('sys'):
            import sys as temp_sys
            temp_sys.setdefaultencoding("iso-8859-1")
            try:
                (s, d), m = xmlrpc.client.loads(utf8)
            finally:
                temp_sys.setdefaultencoding(old_encoding)

        items = list(d.items())
        if have_unicode:
            self.assertEqual(s, "abc \x95")
            self.assertIsInstance(s, str)
            self.assertEqual(items, [("def \x96", "ghi \x97")])
            self.assertIsInstance(items[0][0], str)
            self.assertIsInstance(items[0][1], str)
        else:
            self.assertEqual(s, "abc \xc2\x95")
            self.assertEqual(items, [("def \xc2\x96", "ghi \xc2\x97")])


class HelperTestCase(unittest.TestCase):
    def test_escape(self):
        self.assertEqual(xmlrpc.client.escape("a&b"), "a&amp;b")
        self.assertEqual(xmlrpc.client.escape("a<b"), "a&lt;b")
        self.assertEqual(xmlrpc.client.escape("a>b"), "a&gt;b")

class FaultTestCase(unittest.TestCase):
    def test_repr(self):
        f = xmlrpc.client.Fault(42, 'Test Fault')
        self.assertEqual(repr(f), "<Fault 42: 'Test Fault'>")
        self.assertEqual(repr(f), str(f))

    def test_dump_fault(self):
        f = xmlrpc.client.Fault(42, 'Test Fault')
        s = xmlrpc.client.dumps((f,))
        (newf,), m = xmlrpc.client.loads(s)
        self.assertEqual(newf, {'faultCode': 42, 'faultString': 'Test Fault'})
        self.assertEqual(m, None)

        s = xmlrpc.client.Marshaller().dumps(f)
        self.assertRaises(xmlrpc.client.Fault, xmlrpc.client.loads, s)


class DateTimeTestCase(unittest.TestCase):
    def test_default(self):
        t = xmlrpc.client.DateTime()

    def test_time(self):
        d = 1181399930.036952
        t = xmlrpc.client.DateTime(d)
        self.assertEqual(str(t), time.strftime("%Y%m%dT%H:%M:%S", time.localtime(d)))

    def test_time_tuple(self):
        d = (2007, 6, 9, 10, 38, 50, 5, 160, 0)
        t = xmlrpc.client.DateTime(d)
        self.assertEqual(str(t), '20070609T10:38:50')

    def test_time_struct(self):
        d = time.localtime(1181399930.036952)
        t = xmlrpc.client.DateTime(d)
        self.assertEqual(str(t),  time.strftime("%Y%m%dT%H:%M:%S", d))

    def test_datetime_datetime(self):
        d = datetime.datetime(2007, 1, 2, 3, 4, 5)
        t = xmlrpc.client.DateTime(d)
        self.assertEqual(str(t), '20070102T03:04:05')

    def test_repr(self):
        d = datetime.datetime(2007, 1, 2, 3, 4, 5)
        t = xmlrpc.client.DateTime(d)
        val ="<DateTime '20070102T03:04:05' at %x>" % id(t)
        self.assertEqual(repr(t), val)

    def test_decode(self):
        d = ' 20070908T07:11:13  '
        t1 = xmlrpc.client.DateTime()
        t1.decode(d)
        tref = xmlrpc.client.DateTime(datetime.datetime(2007, 9, 8, 7, 11, 13))
        self.assertEqual(t1, tref)

        t2 = xmlrpc.client._datetime(d)
        self.assertEqual(t1, tref)

class BinaryTestCase(unittest.TestCase):
    def test_default(self):
        t = xmlrpc.client.Binary()
        self.assertEqual(str(t), '')

    def test_string(self):
        d = '\x01\x02\x03abc123\xff\xfe'
        t = xmlrpc.client.Binary(d)
        self.assertEqual(str(t), d)

    def test_decode(self):
        d = '\x01\x02\x03abc123\xff\xfe'
        de = base64.encodestring(d)
        t1 = xmlrpc.client.Binary()
        t1.decode(de)
        self.assertEqual(str(t1), d)

        t2 = xmlrpc.client._binary(de)
        self.assertEqual(str(t2), d)


ADDR = PORT = URL = None

# The evt is set twice.  First when the server is ready to serve.
# Second when the server has been shutdown.  The user must clear
# the event after it has been set the first time to catch the second set.
def http_server(evt, numrequests, requestHandler=None):
    class TestInstanceClass:
        def div(self, x, y):
            return x // y

        def _methodHelp(self, name):
            if name == 'div':
                return 'This is the div function'

    def my_function():
        '''This is my function'''
        return True

    class MyXMLRPCServer(xmlrpc.server.SimpleXMLRPCServer):
        def get_request(self):
            # Ensure the socket is always non-blocking.  On Linux, socket
            # attributes are not inherited like they are on *BSD and Windows.
            s, port = self.socket.accept()
            s.setblocking(True)
            return s, port

        def handle_error(self, request, client_address):
            # test_partial_post causes a close error (as might be
            # expected), apparently because the timing is different
            # between CPython and Jython. So ignore so that the
            # default SocketServer.handle_error logging does not cause
            # issues in unexpected text output in the overall
            # regrtest.
            pass

    if not requestHandler:
        requestHandler = xmlrpc.server.SimpleXMLRPCRequestHandler
    serv = MyXMLRPCServer(("localhost", 0), requestHandler,
                          logRequests=False, bind_and_activate=False)
    try:
        serv.socket.settimeout(3)
        serv.server_bind()
        serv.server_activate()
        global ADDR, PORT, URL
        ADDR, PORT = serv.socket.getsockname()
        #connect to IP address directly.  This avoids socket.create_connection()
        #trying to connect to "localhost" using all address families, which
        #causes slowdown e.g. on vista which supports AF_INET6.  The server listens
        #on AF_INET only.
        URL = "http://%s:%d"%(ADDR, PORT)
        serv.register_introspection_functions()
        serv.register_multicall_functions()
        serv.register_function(pow)
        serv.register_function(lambda x, y: x+y, 'add')
        serv.register_function(my_function)
        serv.register_instance(TestInstanceClass())
        evt.set()

        # handle up to 'numrequests' requests
        while numrequests > 0:
            serv.handle_request()
            numrequests -= 1

    except socket.timeout:
        pass
    finally:
        serv.socket.close()
        PORT = None
        evt.set()

def http_multi_server(evt, numrequests, requestHandler=None):
    class TestInstanceClass:
        def div(self, x, y):
            return x // y

        def _methodHelp(self, name):
            if name == 'div':
                return 'This is the div function'

    def my_function():
        '''This is my function'''
        return True

    class MyXMLRPCServer(xmlrpc.server.MultiPathXMLRPCServer):
        def get_request(self):
            # Ensure the socket is always non-blocking.  On Linux, socket
            # attributes are not inherited like they are on *BSD and Windows.
            s, port = self.socket.accept()
            s.setblocking(True)
            return s, port

    if not requestHandler:
        requestHandler = xmlrpc.server.SimpleXMLRPCRequestHandler
    class MyRequestHandler(requestHandler):
        rpc_paths = []

    serv = MyXMLRPCServer(("localhost", 0), MyRequestHandler,
                          logRequests=False, bind_and_activate=False)
    serv.socket.settimeout(3)
    serv.server_bind()
    try:
        serv.server_activate()
        global ADDR, PORT, URL
        ADDR, PORT = serv.socket.getsockname()
        #connect to IP address directly.  This avoids socket.create_connection()
        #trying to connect to "localhost" using all address families, which
        #causes slowdown e.g. on vista which supports AF_INET6.  The server listens
        #on AF_INET only.
        URL = "http://%s:%d"%(ADDR, PORT)
        paths = ["/foo", "/foo/bar"]
        for path in paths:
            d = serv.add_dispatcher(path, xmlrpc.server.SimpleXMLRPCDispatcher())
            d.register_introspection_functions()
            d.register_multicall_functions()
        serv.get_dispatcher(paths[0]).register_function(pow)
        serv.get_dispatcher(paths[1]).register_function(lambda x, y: x+y, 'add')
        evt.set()

        # handle up to 'numrequests' requests
        while numrequests > 0:
            serv.handle_request()
            numrequests -= 1

    except socket.timeout:
        pass
    finally:
        serv.socket.close()
        PORT = None
        evt.set()

# This function prevents errors like:
#    <ProtocolError for localhost:57527/RPC2: 500 Internal Server Error>
def is_unavailable_exception(e):
    '''Returns True if the given ProtocolError is the product of a server-side
       exception caused by the 'temporarily unavailable' response sometimes
       given by operations on non-blocking sockets.'''

    # sometimes we get a -1 error code and/or empty headers
    try:
        if e.errcode == -1 or e.headers is None:
            return True
        exc_mess = e.headers.get('X-exception')
    except AttributeError:
        # Ignore socket.errors here.
        exc_mess = str(e)

    if exc_mess and 'temporarily unavailable' in exc_mess.lower():
        return True

    return False

@unittest.skipUnless(threading, 'Threading required for this test.')
class BaseServerTestCase(unittest.TestCase):
    requestHandler = None
    request_count = 1
    threadFunc = staticmethod(http_server)

    def setUp(self):
        # enable traceback reporting
        xmlrpc.server.SimpleXMLRPCServer._send_traceback_header = True

        self.evt = threading.Event()
        # start server thread to handle requests
        serv_args = (self.evt, self.request_count, self.requestHandler)
        t = threading.Thread(target=self.threadFunc, args=serv_args)
        t.setDaemon(True)
        t.start()

        # wait for the server to be ready
        self.evt.wait(10)
        self.evt.clear()

    def tearDown(self):
        # wait on the server thread to terminate
        self.evt.wait(10)

        # disable traceback reporting
        xmlrpc.server.SimpleXMLRPCServer._send_traceback_header = False

        # force finalization for tests that rely on deterministic
        # destruction because of ref counting on CPython
        gc.collect()

# NOTE: The tests in SimpleServerTestCase will ignore failures caused by
# "temporarily unavailable" exceptions raised in SimpleXMLRPCServer.  This
# condition occurs infrequently on some platforms, frequently on others, and
# is apparently caused by using SimpleXMLRPCServer with a non-blocking socket
# If the server class is updated at some point in the future to handle this
# situation more gracefully, these tests should be modified appropriately.

class SimpleServerTestCase(BaseServerTestCase):
    def test_simple1(self):
        try:
            p = xmlrpc.client.ServerProxy(URL)
            self.assertEqual(p.pow(6, 8), 6**8)
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    def test_nonascii(self):
        start_string = 'P\N{LATIN SMALL LETTER Y WITH CIRCUMFLEX}t'
        end_string = 'h\N{LATIN SMALL LETTER O WITH HORN}n'

        try:
            p = xmlrpc.client.ServerProxy(URL)
            self.assertEqual(p.add(start_string, end_string),
                             start_string + end_string)
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket unavailable errors.
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    def test_unicode_host(self):
        server = xmlrpc.client.ServerProxy("http://%s:%d/RPC2"%(ADDR, PORT))
        self.assertEqual(server.add("a", "\xe9"), "a\xe9")

    # [ch] The test 404 is causing lots of false alarms.
    def XXXtest_404(self):
        # send POST with httplib, it should return 404 header and
        # 'Not Found' message.
        conn = http.client.HTTPConnection(ADDR, PORT)
        conn.request('POST', '/this-is-not-valid')
        response = conn.getresponse()
        conn.close()

        self.assertEqual(response.status, 404)
        self.assertEqual(response.reason, 'Not Found')

    def test_introspection1(self):
        try:
            p = xmlrpc.client.ServerProxy(URL)
            meth = p.system.listMethods()
            expected_methods = set(['pow', 'div', 'my_function', 'add',
                                    'system.listMethods', 'system.methodHelp',
                                    'system.methodSignature', 'system.multicall'])
            self.assertEqual(set(meth), expected_methods)
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    def test_introspection2(self):
        try:
            # test _methodHelp()
            p = xmlrpc.client.ServerProxy(URL)
            divhelp = p.system.methodHelp('div')
            self.assertEqual(divhelp, 'This is the div function')
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    @unittest.skipIf(sys.flags.optimize >= 2,
                     "Docstrings are omitted with -O2 and above")
    def test_introspection3(self):
        try:
            # test native doc
            p = xmlrpc.client.ServerProxy(URL)
            myfunction = p.system.methodHelp('my_function')
            self.assertEqual(myfunction, 'This is my function')
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    def test_introspection4(self):
        # the SimpleXMLRPCServer doesn't support signatures, but
        # at least check that we can try making the call
        try:
            p = xmlrpc.client.ServerProxy(URL)
            divsig = p.system.methodSignature('div')
            self.assertEqual(divsig, 'signatures not supported')
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    def test_multicall(self):
        try:
            p = xmlrpc.client.ServerProxy(URL)
            multicall = xmlrpc.client.MultiCall(p)
            multicall.add(2, 3)
            multicall.pow(6, 8)
            multicall.div(127, 42)
            add_result, pow_result, div_result = multicall()
            self.assertEqual(add_result, 2+3)
            self.assertEqual(pow_result, 6**8)
            self.assertEqual(div_result, 127//42)
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    def test_non_existing_multicall(self):
        try:
            p = xmlrpc.client.ServerProxy(URL)
            multicall = xmlrpc.client.MultiCall(p)
            multicall.this_is_not_exists()
            result = multicall()

            # result.results contains;
            # [{'faultCode': 1, 'faultString': '<type \'exceptions.Exception\'>:'
            #   'method "this_is_not_exists" is not supported'>}]

            self.assertEqual(result.results[0]['faultCode'], 1)
            self.assertEqual(result.results[0]['faultString'],
                '<type \'exceptions.Exception\'>:method "this_is_not_exists" '
                'is not supported')
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    def test_dotted_attribute(self):
        # Raises an AttributeError because private methods are not allowed.
        self.assertRaises(AttributeError,
                          xmlrpc.server.resolve_dotted_attribute, str, '__add')

        self.assertTrue(xmlrpc.server.resolve_dotted_attribute(str, 'title'))
        # Get the test to run faster by sending a request with test_simple1.
        # This avoids waiting for the socket timeout.
        self.test_simple1()

    def test_partial_post(self):
        # Check that a partial POST doesn't make the server loop: issue #14001.
        conn = http.client.HTTPConnection(ADDR, PORT)
        conn.request('POST', '/RPC2 HTTP/1.0\r\nContent-Length: 100\r\n\r\nbye')
        try:
            conn.close()
        except Exception as e:
            print("Got this exception", type(e), e)

class MultiPathServerTestCase(BaseServerTestCase):
    threadFunc = staticmethod(http_multi_server)
    request_count = 2
    def test_path1(self):
        p = xmlrpc.client.ServerProxy(URL+"/foo")
        self.assertEqual(p.pow(6, 8), 6**8)
        self.assertRaises(xmlrpc.client.Fault, p.add, 6, 8)
    def test_path2(self):
        p = xmlrpc.client.ServerProxy(URL+"/foo/bar")
        self.assertEqual(p.add(6, 8), 6+8)
        self.assertRaises(xmlrpc.client.Fault, p.pow, 6, 8)

#A test case that verifies that a server using the HTTP/1.1 keep-alive mechanism
#does indeed serve subsequent requests on the same connection
class BaseKeepaliveServerTestCase(BaseServerTestCase):
    #a request handler that supports keep-alive and logs requests into a
    #class variable
    class RequestHandler(xmlrpc.server.SimpleXMLRPCRequestHandler):
        parentClass = xmlrpc.server.SimpleXMLRPCRequestHandler
        protocol_version = 'HTTP/1.1'
        myRequests = []
        def handle(self):
            self.myRequests.append([])
            self.reqidx = len(self.myRequests)-1
            return self.parentClass.handle(self)
        def handle_one_request(self):
            result = self.parentClass.handle_one_request(self)
            self.myRequests[self.reqidx].append(self.raw_requestline)
            return result

    requestHandler = RequestHandler
    def setUp(self):
        #clear request log
        self.RequestHandler.myRequests = []
        return BaseServerTestCase.setUp(self)

#A test case that verifies that a server using the HTTP/1.1 keep-alive mechanism
#does indeed serve subsequent requests on the same connection
class KeepaliveServerTestCase1(BaseKeepaliveServerTestCase):
    def test_two(self):
        p = xmlrpc.client.ServerProxy(URL)
        #do three requests.
        self.assertEqual(p.pow(6, 8), 6**8)
        self.assertEqual(p.pow(6, 8), 6**8)
        self.assertEqual(p.pow(6, 8), 6**8)

        #they should have all been handled by a single request handler
        self.assertEqual(len(self.RequestHandler.myRequests), 1)

        #check that we did at least two (the third may be pending append
        #due to thread scheduling)
        self.assertGreaterEqual(len(self.RequestHandler.myRequests[-1]), 2)

#test special attribute access on the serverproxy, through the __call__
#function.
class KeepaliveServerTestCase2(BaseKeepaliveServerTestCase):
    #ask for two keepalive requests to be handled.
    request_count=2

    def test_close(self):
        p = xmlrpc.client.ServerProxy(URL)
        #do some requests with close.
        self.assertEqual(p.pow(6, 8), 6**8)
        self.assertEqual(p.pow(6, 8), 6**8)
        self.assertEqual(p.pow(6, 8), 6**8)
        p("close")() #this should trigger a new keep-alive request
        self.assertEqual(p.pow(6, 8), 6**8)
        self.assertEqual(p.pow(6, 8), 6**8)
        self.assertEqual(p.pow(6, 8), 6**8)

        #they should have all been two request handlers, each having logged at least
        #two complete requests
        self.assertEqual(len(self.RequestHandler.myRequests), 2)
        self.assertGreaterEqual(len(self.RequestHandler.myRequests[-1]), 2)
        self.assertGreaterEqual(len(self.RequestHandler.myRequests[-2]), 2)

    def test_transport(self):
        p = xmlrpc.client.ServerProxy(URL)
        #do some requests with close.
        self.assertEqual(p.pow(6, 8), 6**8)
        p("transport").close() #same as above, really.
        self.assertEqual(p.pow(6, 8), 6**8)
        self.assertEqual(len(self.RequestHandler.myRequests), 2)

#A test case that verifies that gzip encoding works in both directions
#(for a request and the response)
class GzipServerTestCase(BaseServerTestCase):
    #a request handler that supports keep-alive and logs requests into a
    #class variable
    class RequestHandler(xmlrpc.server.SimpleXMLRPCRequestHandler):
        parentClass = xmlrpc.server.SimpleXMLRPCRequestHandler
        protocol_version = 'HTTP/1.1'

        def do_POST(self):
            #store content of last request in class
            self.__class__.content_length = int(self.headers["content-length"])
            return self.parentClass.do_POST(self)
    requestHandler = RequestHandler

    class Transport(xmlrpc.client.Transport):
        #custom transport, stores the response length for our perusal
        fake_gzip = False
        def parse_response(self, response):
            self.response_length=int(response.getheader("content-length", 0))
            return xmlrpc.client.Transport.parse_response(self, response)

        def send_content(self, connection, body):
            if self.fake_gzip:
                #add a lone gzip header to induce decode error remotely
                connection.putheader("Content-Encoding", "gzip")
            return xmlrpc.client.Transport.send_content(self, connection, body)

    def setUp(self):
        BaseServerTestCase.setUp(self)

    def test_gzip_request(self):
        t = self.Transport()
        t.encode_threshold = None
        p = xmlrpc.client.ServerProxy(URL, transport=t)
        self.assertEqual(p.pow(6, 8), 6**8)
        a = self.RequestHandler.content_length
        t.encode_threshold = 0 #turn on request encoding
        self.assertEqual(p.pow(6, 8), 6**8)
        b = self.RequestHandler.content_length
        self.assertTrue(a>b)

    def test_bad_gzip_request(self):
        t = self.Transport()
        t.encode_threshold = None
        t.fake_gzip = True
        p = xmlrpc.client.ServerProxy(URL, transport=t)
        cm = self.assertRaisesRegex(xmlrpc.client.ProtocolError,
                                     re.compile(r"\b400\b"))
        with cm:
            p.pow(6, 8)

    def test_gsip_response(self):
        t = self.Transport()
        p = xmlrpc.client.ServerProxy(URL, transport=t)
        old = self.requestHandler.encode_threshold
        self.requestHandler.encode_threshold = None #no encoding
        self.assertEqual(p.pow(6, 8), 6**8)
        a = t.response_length
        self.requestHandler.encode_threshold = 0 #always encode
        self.assertEqual(p.pow(6, 8), 6**8)
        b = t.response_length
        self.requestHandler.encode_threshold = old
        self.assertTrue(a>b)

#Test special attributes of the ServerProxy object
class ServerProxyTestCase(unittest.TestCase):
    def setUp(self):
        unittest.TestCase.setUp(self)
        if threading:
            self.url = URL
        else:
            # Without threading, http_server() and http_multi_server() will not
            # be executed and URL is still equal to None. 'http://' is a just
            # enough to choose the scheme (HTTP)
            self.url = 'http://'

    def test_close(self):
        p = xmlrpc.client.ServerProxy(self.url)
        self.assertEqual(p('close')(), None)

    def test_transport(self):
        t = xmlrpc.client.Transport()
        p = xmlrpc.client.ServerProxy(self.url, transport=t)
        self.assertEqual(p('transport'), t)

# This is a contrived way to make a failure occur on the server side
# in order to test the _send_traceback_header flag on the server
class FailingMessageClass(mimetools.Message):
    def __getitem__(self, key):
        key = key.lower()
        if key == 'content-length':
            return 'I am broken'
        return mimetools.Message.__getitem__(self, key)


@unittest.skipUnless(threading, 'Threading required for this test.')
class FailingServerTestCase(unittest.TestCase):
    def setUp(self):
        self.evt = threading.Event()
        # start server thread to handle requests
        serv_args = (self.evt, 1)
        threading.Thread(target=http_server, args=serv_args).start()

        # wait for the server to be ready
        self.evt.wait()
        self.evt.clear()

    def tearDown(self):
        # wait on the server thread to terminate
        self.evt.wait()
        # reset flag
        xmlrpc.server.SimpleXMLRPCServer._send_traceback_header = False
        # reset message class
        xmlrpc.server.SimpleXMLRPCRequestHandler.MessageClass = mimetools.Message

    def test_basic(self):
        # check that flag is false by default
        flagval = xmlrpc.server.SimpleXMLRPCServer._send_traceback_header
        self.assertEqual(flagval, False)

        # enable traceback reporting
        xmlrpc.server.SimpleXMLRPCServer._send_traceback_header = True

        # test a call that shouldn't fail just as a smoke test
        try:
            p = xmlrpc.client.ServerProxy(URL)
            self.assertEqual(p.pow(6, 8), 6**8)
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e):
                # protocol error; provide additional information in test output
                self.fail("%s\n%s" % (e, getattr(e, "headers", "")))

    def test_fail_no_info(self):
        # use the broken message class
        xmlrpc.server.SimpleXMLRPCRequestHandler.MessageClass = FailingMessageClass

        try:
            p = xmlrpc.client.ServerProxy(URL)
            p.pow(6, 8)
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e) and hasattr(e, "headers"):
                # The two server-side error headers shouldn't be sent back in this case
                self.assertTrue(e.headers.get("X-exception") is None)
                self.assertTrue(e.headers.get("X-traceback") is None)
        else:
            self.fail('ProtocolError not raised')

    def test_fail_with_info(self):
        # use the broken message class
        xmlrpc.server.SimpleXMLRPCRequestHandler.MessageClass = FailingMessageClass

        # Check that errors in the server send back exception/traceback
        # info when flag is set
        xmlrpc.server.SimpleXMLRPCServer._send_traceback_header = True

        try:
            p = xmlrpc.client.ServerProxy(URL)
            p.pow(6, 8)
        except (xmlrpc.client.ProtocolError, socket.error) as e:
            # ignore failures due to non-blocking socket 'unavailable' errors
            if not is_unavailable_exception(e) and hasattr(e, "headers"):
                # We should get error info in the response
                expected_err = "invalid literal for int() with base 10: 'I am broken'"
                self.assertEqual(e.headers.get("x-exception"), expected_err)
                self.assertTrue(e.headers.get("x-traceback") is not None)
        else:
            self.fail('ProtocolError not raised')

class CGIHandlerTestCase(unittest.TestCase):
    def setUp(self):
        self.cgi = xmlrpc.server.CGIXMLRPCRequestHandler()

    def tearDown(self):
        self.cgi = None

    def test_cgi_get(self):
        with support.EnvironmentVarGuard() as env:
            env['REQUEST_METHOD'] = 'GET'
            # if the method is GET and no request_text is given, it runs handle_get
            # get sysout output
            with support.captured_stdout() as data_out:
                self.cgi.handle_request()

            # parse Status header
            data_out.seek(0)
            handle = data_out.read()
            status = handle.split()[1]
            message = ' '.join(handle.split()[2:4])

            self.assertEqual(status, '400')
            self.assertEqual(message, 'Bad Request')

    def test_cgi_xmlrpc_response(self):
        data = """<?xml version='1.0'?>
        <methodCall>
            <methodName>test_method</methodName>
            <params>
                <param>
                    <value><string>foo</string></value>
                </param>
                <param>
                    <value><string>bar</string></value>
                </param>
            </params>
        </methodCall>
        """

        with support.EnvironmentVarGuard() as env, \
             support.captured_stdout() as data_out, \
             support.captured_stdin() as data_in:
            data_in.write(data)
            data_in.seek(0)
            env['CONTENT_LENGTH'] = str(len(data))
            self.cgi.handle_request()
        data_out.seek(0)

        # will respond exception, if so, our goal is achieved ;)
        handle = data_out.read()

        # start with 44th char so as not to get http header, we just need only xml
        self.assertRaises(xmlrpc.client.Fault, xmlrpc.client.loads, handle[44:])

        # Also test the content-length returned  by handle_request
        # Using the same test method inorder to avoid all the datapassing
        # boilerplate code.
        # Test for bug: http://bugs.python.org/issue5040

        content = handle[handle.find("<?xml"):]

        self.assertEqual(
            int(re.search('Content-Length: (\d+)', handle).group(1)),
            len(content))


class FakeSocket:

    def __init__(self):
        self.data = io.StringIO()

    def send(self, buf):
        self.data.write(buf)
        return len(buf)

    def sendall(self, buf):
        self.data.write(buf)

    def getvalue(self):
        return self.data.getvalue()

    def makefile(self, x='r', y=-1):
        raise RuntimeError

    def close(self):
        pass

class FakeTransport(xmlrpc.client.Transport):
    """A Transport instance that records instead of sending a request.

    This class replaces the actual socket used by httplib with a
    FakeSocket object that records the request.  It doesn't provide a
    response.
    """

    def make_connection(self, host):
        conn = xmlrpc.client.Transport.make_connection(self, host)
        conn.sock = self.fake_socket = FakeSocket()
        return conn

class TransportSubclassTestCase(unittest.TestCase):

    def issue_request(self, transport_class):
        """Return an HTTP request made via transport_class."""
        transport = transport_class()
        proxy = xmlrpc.client.ServerProxy("http://example.com/",
                                      transport=transport)
        try:
            proxy.pow(6, 8)
        except RuntimeError:
            return transport.fake_socket.getvalue()
        return None

    def test_custom_user_agent(self):
        class TestTransport(FakeTransport):

            def send_user_agent(self, conn):
                xmlrpc.client.Transport.send_user_agent(self, conn)
                conn.putheader("X-Test", "test_custom_user_agent")

        req = self.issue_request(TestTransport)
        self.assertIn("X-Test: test_custom_user_agent\r\n", req)

    def test_send_host(self):
        class TestTransport(FakeTransport):

            def send_host(self, conn, host):
                xmlrpc.client.Transport.send_host(self, conn, host)
                conn.putheader("X-Test", "test_send_host")

        req = self.issue_request(TestTransport)
        self.assertIn("X-Test: test_send_host\r\n", req)

    def test_send_request(self):
        class TestTransport(FakeTransport):

            def send_request(self, conn, url, body):
                xmlrpc.client.Transport.send_request(self, conn, url, body)
                conn.putheader("X-Test", "test_send_request")

        req = self.issue_request(TestTransport)
        self.assertIn("X-Test: test_send_request\r\n", req)

    def test_send_content(self):
        class TestTransport(FakeTransport):

            def send_content(self, conn, body):
                conn.putheader("X-Test", "test_send_content")
                xmlrpc.client.Transport.send_content(self, conn, body)

        req = self.issue_request(TestTransport)
        self.assertIn("X-Test: test_send_content\r\n", req)

@support.reap_threads
def test_main():
    xmlrpc_tests = [XMLRPCTestCase, HelperTestCase, DateTimeTestCase,
         BinaryTestCase, FaultTestCase, TransportSubclassTestCase]
    xmlrpc_tests.append(SimpleServerTestCase)
    xmlrpc_tests.append(KeepaliveServerTestCase1)
    xmlrpc_tests.append(KeepaliveServerTestCase2)
    try:
        import gzip
        xmlrpc_tests.append(GzipServerTestCase)
    except ImportError:
        pass #gzip not supported in this build
    xmlrpc_tests.append(MultiPathServerTestCase)
    xmlrpc_tests.append(ServerProxyTestCase)
    xmlrpc_tests.append(FailingServerTestCase)
    xmlrpc_tests.append(CGIHandlerTestCase)

    support.run_unittest(*xmlrpc_tests)

if __name__ == "__main__":
    test_main()

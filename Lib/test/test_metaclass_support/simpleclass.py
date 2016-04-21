from metaclass import NoOpMetaClass
class TestClass(object, metaclass=NoOpMetaClass):
    pass

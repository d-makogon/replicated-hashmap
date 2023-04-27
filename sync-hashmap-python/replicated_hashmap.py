class ReplicatedHashMap():
    def __init__(self):
        self.map = {}
    
    def get(self, key):
        return self.map[key]

    def insert(self, key, value):
        self.map[key] = value

    def update(self, key, value):
        self.map[key] = value

    def remove(self, key):
        del self.map[key]

    def all(self):
        return self.map.items()
